package cz.cvut.fel.bulkodav.view;

import cz.cvut.fel.bulkodav.communication.NodeInfo;
import cz.cvut.fel.bulkodav.exceptions.ConnectingException;
import cz.cvut.fel.bulkodav.node.Node;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectController
{
    @FXML
    TextField yourNameField;
    @FXML
    TextField yourIpField;
    @FXML
    Spinner<Integer> yourPortField;
    @FXML
    CheckBox isNewCheckBox;
    @FXML
    TextField leaderIpField;
    @FXML
    Spinner<Integer> leaderPortField;

    private final PseudoClass errorClass = PseudoClass.getPseudoClass("error");
    private Alert alert;

    /**
     * Initializes the class with a default data.
     */
    @FXML
    public void initialize()
    {
        alert = new Alert(Alert.AlertType.ERROR);
        yourPortField.getValueFactory().setValue(6000);
        leaderPortField.getValueFactory().setValue(6000);
    }

    /**
     * Initialized the class with the provided data when the user logs off and return to the view this controller handles.
     *
     * @param nodeInfo    The information about the node that the controller uses to fill fields.
     * @param isKing      The information about whether the node which logged of was a king node.
     * @param kingsParams The connection parameters of a leader node. If this node was the leader, they are not used.
     */
    void initializeAfterLogOff(NodeInfo nodeInfo, boolean isKing, String kingsParams)
    {
        alert = new Alert(Alert.AlertType.ERROR);
        yourNameField.setText(nodeInfo.getName());
        yourIpField.setText(nodeInfo.getAddress());
        yourPortField.getValueFactory().setValue(nodeInfo.getPort());
        if (isKing)
        {
            isNewCheckBox.setSelected(true);
        } else
        {
            isNewCheckBox.setSelected(false);
            String[] params = kingsParams.split(":");
            leaderIpField.setDisable(false);
            leaderIpField.setText(params[0]);
            leaderPortField.setDisable(false);
            leaderPortField.getValueFactory().setValue(Integer.parseInt(params[1]));
        }
    }

    /**
     * Hanldes click on the isNew checkbox.
     *
     * @param event The click event on the isNew checkbox.
     */
    @FXML
    private void handleIsNewClicked(MouseEvent event)
    {
        if (isNewCheckBox.isSelected())
        {
            leaderIpField.setDisable(true);
            leaderPortField.setDisable(true);
        } else
        {
            leaderIpField.setDisable(false);
            leaderPortField.setDisable(false);
        }
    }

    /**
     * Handles the connection after click on connect button.
     *
     * @param event the event raised after click on connect button.
     * @throws IOException if the view can't be switched to the chat view.
     */
    @FXML
    private void handleConnectClicked(ActionEvent event) throws IOException
    {
        if (invalidInput())
        {
            return;
        }

        NodeInfo myInfo = new NodeInfo(yourIpField.getText(), yourPortField.getValue(), yourNameField.getText());
        if (isNewCheckBox.isSelected())
        {
            try
            {
                Node node = new Node();
                node.startNewNetwork(myInfo);
                switchToChatView(event, node);
            } catch (ConnectingException e)
            {
                alert.setContentText(e.getMessage());
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
            }
        } else
        {
            try
            {
                Node node = new Node();
                node.connectToRemoteKing(new NodeInfo(leaderIpField.getText(), leaderPortField.getValue(), ""), myInfo);
                switchToChatView(event, node);
            } catch (ConnectingException e)
            {
                alert.setContentText(e.getMessage());
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
            }
        }
    }

    /**
     * Gets a value which represents whther the user input was invalid.
     *
     * @return a value which represents whther the user input was invalid.
     */
    private boolean invalidInput()
    {
        return invalidName(yourNameField) + invalidIp(yourIpField) + invalidIp(leaderIpField) > 0;
    }

    /**
     * Checks whether the name in the text field is invalid.
     *
     * @param tf the text field containing the name.
     * @return 1 if the name was invalid, otherwise returns 0;
     */
    private int invalidName(TextField tf)
    {
        ObservableList<String> styleClass = tf.getStyleClass();
        if (tf.getText().contains(" ") || tf.getText().trim().length() == 0)
        {
            if (!styleClass.contains("error"))
            {
                tf.pseudoClassStateChanged(errorClass, true);
                alert.setContentText("The name must not be empty or contain spaces!");
                alert.show();
            }
            return 1;
        } else
        {
            tf.pseudoClassStateChanged(errorClass, false);
            return 0;
        }
    }

    /**
     * Checks whether the ip address in the text field is invalid.
     *
     * @param tf the text field containing the ip address.
     * @return 1 if the ip address was invalid, otherwise returns 0;
     */
    private int invalidIp(TextField tf)
    {
        ObservableList<String> styleClass = tf.getStyleClass();
        if (!checkIPv4(tf.getText()) && !tf.isDisabled())
        {
            if (!styleClass.contains("error"))
            {
                tf.pseudoClassStateChanged(errorClass, true);
                alert.setContentText("The ip address is not in the right format. Use \"0.0.0.0\" format. ");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.show();
            }
            return 1;
        } else
        {
            tf.pseudoClassStateChanged(errorClass, false);
            return 0;
        }
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
     * Switches the view to the chat.
     * @param event The click on the connect button event.
     * @param node The network representation of the user.
     * @throws IOException if the chat view cannot be loaded.
     */
    private void switchToChatView(ActionEvent event, Node node) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxmls/chat.fxml"));
        Parent chatView = loader.load();
        Scene chatViewScene = new Scene(chatView);
        ChatController controller = loader.getController();
        Stage window = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

        controller.initialize(node, window);
        window.setScene(chatViewScene);
        window.show();
    }
}
