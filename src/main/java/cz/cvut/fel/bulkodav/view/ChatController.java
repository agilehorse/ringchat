package cz.cvut.fel.bulkodav.view;

import cz.cvut.fel.bulkodav.communication.TopologyInfo;
import cz.cvut.fel.bulkodav.exceptions.OperationException;
import cz.cvut.fel.bulkodav.node.Node;
import cz.cvut.fel.bulkodav.node.UserStateChange;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class ChatController
{
    @FXML
    Label userNameLabel;
    @FXML
    Button logOffButton;
    @FXML
    TextArea chatArea;
    @FXML
    TableView<TopologyInfo> topologyTable;
    @FXML
    TableColumn<TopologyInfo, String> nodeNameColumn;
    @FXML
    TableColumn<TopologyInfo, String> leftNodeNameColumn;
    @FXML
    TableColumn<TopologyInfo, String> rightNodeNameColumn;
    @FXML
    Button refreshButton;
    @FXML
    TextField messageField;
    @FXML
    Button sendButton;

    private Node node;
    private final ExecutorService nodeExecutor = Executors.newSingleThreadExecutor();
    private Alert alert;

    /**
     * Initializes the class with the default data.
     */
    @FXML
    public void initialize()
    {
        chatArea.setWrapText(true);
        alert = new Alert(Alert.AlertType.ERROR);
    }

    /**
     * Initializes the class with the provided data.
     *
     * @param node   The representation of a user in the network.
     * @param window The main application window.
     */
    void initialize(Node node, Stage window)
    {
        window.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::checkBeforeClosingWindow);
        chatArea.setWrapText(true);
        alert = new Alert(Alert.AlertType.ERROR);
        node.setDaemon(true);
        this.node = node;
        this.node.setUiController(this);
        nodeExecutor.execute(this.node);
        nodeNameColumn.setCellValueFactory(new PropertyValueFactory<>("nodeName"));
        leftNodeNameColumn.setCellValueFactory(new PropertyValueFactory<>("leftName"));
        rightNodeNameColumn.setCellValueFactory(new PropertyValueFactory<>("rightName"));
        userNameLabel.setText(node.getNodeName());
        if (this.node.isKing())
        {
            refreshTopologyInfo(null);
        }
    }

    /**
     * Refreshes data on UI when a connection state of a user changes.
     *
     * @param userStateChange The information about a change of user.
     */
    public void refreshData(UserStateChange userStateChange)
    {
        ObservableList<TopologyInfo> info = FXCollections.observableArrayList();
        info.addAll(userStateChange.getCurrentTopologyInfos());
        topologyTable.setItems(info);
        String changedUser = userStateChange.getUserName();
        if (changedUser != null)
        {
            String userState = changedUser + " is " + userStateChange.getConnectionState() + "\n";
            chatArea.setText(chatArea.getText() + userState);
        }
    }

    /**
     * Displays exception raised on backend in an alert dialog.
     *
     * @param errorMessage The error message.
     */
    public void exceptionRaised(String errorMessage)
    {
        alert.setContentText(errorMessage);
        alert.show();
    }

    /**
     * Adds a new message to the chat.
     *
     * @param text The text content of the message.
     */
    public void displayNewChatMessage(String text)
    {
        chatArea.setText(chatArea.getText() + text);
    }

    /**
     * Sends a message to the chat after pressing enter in message field.
     * @param event enter key pressed event.
     */
    @FXML
    private void onEnter(ActionEvent event)
    {
        try
        {
            node.sendMessage(messageField.getText());
        } catch (OperationException e)
        {
            alert.setContentText(e.getMessage());
            alert.show();
        }
        messageField.clear();
    }

    /**
     * Refreshes the topology info.
     * @param event the action even of the refresh button.
     */
    @FXML
    private void refreshTopologyInfo(ActionEvent event)
    {
        try
        {
            ObservableList<TopologyInfo> info = FXCollections.observableArrayList();
            info.addAll(node.getTopologyInfo());
            topologyTable.setItems(info);
        } catch (OperationException e)
        {
            alert.setContentText(e.getMessage());
            alert.show();
        }
    }

    /**
     * Sends the message to the chat.
     * @param event the action event of the send button.
     */
    @FXML
    private void sendMessage(ActionEvent event)
    {
        try
        {
            node.sendMessage(messageField.getText());
        } catch (OperationException e)
        {
            alert.setContentText(e.getMessage());
            alert.show();
        }
        messageField.clear();
    }

    /**
     * Logs off the user after clicking on a log off button.
     * @param event the event raised by click on the log off button.
     * @throws IOException if the log off fails.
     */
    @FXML
    private void logOff(ActionEvent event) throws IOException
    {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/fxmls/connect.fxml"));
        Parent chatView = loader.load();
        Scene chatViewScene = new Scene(chatView);
        ConnectController controller = loader.getController();
        controller.initializeAfterLogOff(node.getNodeInfo(), node.isKing(), node.getKingsConnectionParams());
        disconnectNode();
        Stage window = (Stage) sendButton.getScene().getWindow();
        window.setScene(chatViewScene);
        window.show();
    }

    /**
     * Opens a confirmation dialog to user to close the connection before exiting the application by X.
     * @param event window closing event.
     */
    private void checkBeforeClosingWindow(WindowEvent event)
    {
        if (node.isLoggedIn())
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            Stage stage = (Stage) event.getSource();
            alert.getButtonTypes().remove(ButtonType.OK);
            alert.getButtonTypes().add(ButtonType.CANCEL);
            alert.getButtonTypes().add(ButtonType.YES);
            alert.setTitle("Quit application");
            alert.setContentText("In order to close the application you need to log off. Do you want to log off right now?");
            alert.initOwner(stage.getOwner());
            Optional<ButtonType> res = alert.showAndWait();

            if (res.isPresent())
            {
                if (res.get().equals(ButtonType.CANCEL))
                    event.consume();
                else disconnectNode();
            }
        }
    }

    /**
     * Disconnects the node user from the chat.
     */
    private void disconnectNode()
    {
        node.die();
        nodeExecutor.shutdown();
    }
}
