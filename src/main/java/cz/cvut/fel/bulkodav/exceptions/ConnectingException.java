package cz.cvut.fel.bulkodav.exceptions;

/**
 * The {@link ConnectingException} is raised when the initiation of connection with a node fails.
 */
public class ConnectingException extends Exception
{
    private final String message;

    /**
     * The constructor for {@link ConnectingException} class.
     *
     * @param message The message.
     */
    public ConnectingException(String message)
    {
        this.message = message;
    }

    /**
     * Gets the exception message.
     *
     * @return the exception mesage.
     */
    @Override
    public String getMessage()
    {
        return message;
    }
}
