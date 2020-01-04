package cz.cvut.fel.bulkodav;

import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.exceptions.ConnectingException;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import cz.cvut.fel.bulkodav.node.Node;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@link CommandHandler} class provides an ability for a user to interact with other nodes through
 * commands given through a command line.
 */
public class CommandHandler
{
    private final BufferedReader reader;
    private final PrintWriter writer;
    private Node node;
    private boolean loggedIn;
    private boolean finished;
    private final static Logger logger = Logger.getLogger(CommandHandler.class);
    private final ExecutorService nodeRunner = Executors.newSingleThreadExecutor();

    /**
     * The constructor for {@link cz.cvut.fel.bulkodav.CommandHandler} class.
     */
    public CommandHandler()
    {
        reader = new BufferedReader(new InputStreamReader(System.in));
        writer = new PrintWriter(new OutputStreamWriter(System.out), true);
    }

    /**
     * Runs and listens to the commands inputted by the user in the command line.
     */
    void listen()
    {
        logger.info("To login, please type \"in\".");
        while (!finished)
        {
            try
            {
                if (reader.ready())
                {
                    switch (reader.readLine())
                    {
                        case "in":
                            handleLogin();
                            break;
                        case "message":
                            createNewMessage();
                            break;
                        case "users":
                            checkOnlineUsers();
                            break;
                        case "commands":
                            printCommands();
                            break;
                        case "off":
                            handleLogoff();
                            break;
                        case "info":
                            if (loggedIn)
                            {
                                node.handleTopologyInfoPrinting();
                            } else
                            {
                                logger.error("\n- You need to be logged in to check info topology info.");
                            }
                            break;
                        case "":
                            logger.info("Node " + node.getNodeInfo());
                            break;
                        default:
                            logger.error("\n- Command not found. Did you write it correctly?");
                            break;
                    }
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles login of the user.
     */
    private void handleLogin() throws IOException
    {
        if (loggedIn)
        {
            logger.error("You are already logged in.");
        } else
        {
            logger.info("Enter your name: (no spaces)");
            String myName = reader.readLine();
            if (myName == null || myName.contains(" ") || myName.equals(""))
            {
                logger.error("The name is invalid... Aborting. " +
                        "Input \"in\" command if your wish to login again.");
                return;
            }

            logger.info("In order to connect you need to enter your network parameters.");
            NodeInfo myId = createInfoFromInput(myName);
            if (myId == null) return;

            logger.info("Enter number \"1\" if you wish to create a new network. " +
                    "Enter number \"2\" if you wish to connect to an existing node. " +
                    "Any other input will cancel login.");
            String mode = reader.readLine();
            node = new Node();
            chooseConnectionMode(myId, mode);
        }
    }

    /**
     * Chooses to start a new network or connect to an existing one.
     * @param myInfo the connection info of the user.
     * @param mode The connection mode.
     */
    private void chooseConnectionMode(NodeInfo myInfo, String mode)
    {
        switch (mode)
        {
            case "1":
                try
                {
                    node.startNewNetwork(myInfo);
                    loggedIn = true;
                    node.setDaemon(true);
                    nodeRunner.execute(node);
                    printCommands();
                } catch (ConnectingException e)
                {
                    logger.error(e.getMessage());
                }
                break;

            case "2":
                logger.info("Enter ip address the remote leader should be running on.");
                NodeInfo kingsId = createInfoFromInput("");
                if (kingsId == null) return;
                if (kingsId.getAddress().equals(myInfo.getAddress()) && kingsId.getPort() == myInfo.getPort())
                {
                    logger.info("You cannot connect to yourself. Aborting");
                    return;
                }

                try
                {
                    node.connectToRemoteKing(kingsId, myInfo);
                    loggedIn = true;
                    node.setDaemon(true);
                    nodeRunner.execute(node);
                    printCommands();
                } catch (ConnectingException e)
                {
                    logger.error(e.getMessage());
                }
                break;

            default:
                logger.info("Cancelling the login.");
                break;
        }
    }

    /**
     * Handles the log off of the node.
     *
     * @throws IOException if there was some error while logging off.
     */
    private void handleLogoff() throws IOException
    {
        if (!loggedIn)
        {
            logger.error("\n- You need to be logged in before you log off.");
        } else
        {
            finished = true;
            logger.info("\n- Logging off...");
            node.die();
            nodeRunner.shutdown();
            writer.close();
            reader.close();
            logger.info("\n- Logoff finished.");
        }
    }

    /**
     * Creates the identification of the user, by the provided name, asks for ip address and port.
     *
     * @param name The name of the user.
     * @return The info containing connection parameters of the user.
     */
    private NodeInfo createInfoFromInput(String name)
    {
        String ipAddress;
        try
        {
            logger.info("Enter the ip address.");
            ipAddress = reader.readLine();
            if (!checkIPv4(ipAddress))
            {
                logger.error("Invalid ip address. It should be in the format: \"0.0.0.0\". Aborting.");
                return null;
            }
        } catch (IOException e)
        {
            logger.error("Invalid input. Aborting.");
            return null;
        }

        int port;
        try
        {
            logger.info("Input the port. It should be in the range 1024-65535");
            String str = reader.readLine();
            port = Integer.parseInt(str);
            if (port < 1024 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException | IOException e)
        {
            logger.error("Invalid input. Port must be a positive whole number. Aborting.");
            return null;
        }

        return new NodeInfo(ipAddress, port, name);
    }

    /**
     * Checks if the ip address is in IPv4 format.
     *
     * @param ip The ip address in string representation.
     * @return true if the address is in IPv4, otherwise false.
     */
    private static boolean checkIPv4(final String ip)
    {
        try
        {
            final InetAddress address = InetAddress.getByName(ip);
            return address.getHostAddress().equals(ip)
                    && address instanceof Inet4Address;
        } catch (final UnknownHostException e)
        {
            return false;
        }
    }

    /**
     * Prints the available commands to the user.
     */
    private void printCommands()
    {
        logger.info("\n- The commands are: \"message\" - to send a message, " +
                "\"users\" - to list available users \"info\" to check topology info " +
                "\"commands\" to print this exact message and \"off\" - to log off.");
    }

    /**
     * Handles the creation of the new message which the user wants to send to the other node.
     * @throws IOException if reading input from the user fails.
     */
    private void createNewMessage() throws IOException
    {
        if (!loggedIn)
        {
            logger.error("\n- You need to be logged in to send a message.");
        } else
        {
            logger.info("\n- Write the message: ");
            trySendingAMessage(reader.readLine());
        }
    }

    /**
     * Asks for the current online users
     */
    private void checkOnlineUsers()
    {
        if (!loggedIn)
        {
            logger.error("\n- You need to be logged in to list online users.");
        } else
        {
            try
            {
                logger.info("Online users are: " + node.getOnlineUsers());
            } catch (OperationException e)
            {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * Tries to send the message to the chat.
     *
     * @param content The content of the message.
     */
    private void trySendingAMessage(String content)
    {
        try
        {
            node.sendMessage(content);
        } catch (OperationException e)
        {
            logger.error(e.getMessage());
        }
    }
}
