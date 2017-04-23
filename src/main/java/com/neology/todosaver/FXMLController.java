package com.neology.todosaver;

import abstracts.LocalEnvironment;
import com.neology.database.DatabaseController;
import com.neology.exceptions.TransportException;
import com.neology.interfaces.Viewable;
import com.neology.net.BaudrateMeter;
import com.neology.net.NetController;
import com.neology.net.Transport;
import com.neology.xml.XMLController;
import java.awt.AWTException;
import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class FXMLController extends LocalEnvironment implements Initializable,Viewable {
    
    @FXML
    private Label VER;
    @FXML
    private ListView MAIN_VIEW;
    @FXML
    private TextArea NOTE_VIEW;
    @FXML
    private TextField LOGIN;
    @FXML
    private PasswordField PASS;
    @FXML
    private Button LOGIN_BUTTON,SETTINGS,ABOUT,CONNECT,DISCONNECT,LOG_OUT_BUTTON,REFRESH;
    @FXML
    private GridPane VIEWER_PANEL;
    String ACTUAL_NAME;
    
    protected DatabaseController DB;
    protected ArrayList<String> INIT;
    protected ArrayList<String> POOL = new ArrayList<>();
    protected boolean IS_LOGGED_IN = false;
    protected String LOGGED_IN = "";
    protected boolean IS_CONNECTED = false;
    //final Connector connector = new Connector();
    private ArrayList<Image> IMG_BUFFER = new ArrayList<>();
    static ConcurrentHashMap<Transport,BaudrateMeter>  TRANSPORTERS = new ConcurrentHashMap<>();
    
    final Executor EXEC = Executors.newSingleThreadExecutor();            
    ObservableList<Label> list = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        LOGIN_BUTTON.setOnAction(event ->{
            try {
                if(checkIfInitExists()){
                    setUpConfiguration();
                    initTransporters("192.168.0.0/24",7999);
                }
                loginUser();
            } catch (Exception ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        REFRESH.setOnAction(event ->{
            
        });
        
        LOG_OUT_BUTTON.setOnAction(event ->{
            LOGGED_IN = "root";
            DISCONNECT.fire();
            IS_LOGGED_IN = false;
            CONNECT.setDisable(true);
            DISCONNECT.setDisable(true);
            SETTINGS.setDisable(true);
            REFRESH.setDisable(true);
        });
        
        SETTINGS.setOnAction(event ->{
            SettingsForm settings = new SettingsForm();
            try {
                settings.start(new Stage());
            } catch (Exception ex) {
                Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        CONNECT.setOnAction(event ->{
            NetController net = new NetController();
                if(!IS_CONNECTED){ 
                    try {
                        downloadAndSetToModel();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        });
        
        DISCONNECT.setOnAction(event ->{
                IS_CONNECTED = false;
        });
        
    }   

    
    private boolean checkIfInitExists(){
        return new File("init.xml").exists();
    }
    
    private void setUpConfiguration() throws SAXException, IOException, ParserConfigurationException, ClassNotFoundException, SQLException{
            XMLController xml = new XMLController();
            INIT = xml.parseInitFile();
    }

    private void loginUser() throws SQLException, ClassNotFoundException {
        if(LOGIN.getText() != null&PASS.getText() != null){
            if(LOGIN.getText().equals("root")&PASS.getText().equals("q@wertyuiop")){
                viewAlert("Login","Logging in","Logged in as root.",AlertType.INFORMATION);
                IS_LOGGED_IN = true;
                CONNECT.setDisable(false);
                DISCONNECT.setDisable(false);
                SETTINGS.setDisable(false);
                REFRESH.setDisable(false);
            }else{
               //XML parser here
                if(!LOGGED_IN.isEmpty()){
                    viewAlert("Login","Logging in","Logged in as "+LOGGED_IN,AlertType.INFORMATION);
                    CONNECT.setDisable(false);
                    DISCONNECT.setDisable(false);
                    SETTINGS.setDisable(false);
                    REFRESH.setDisable(false);
                }else{
                    viewError("Error. This user hasn't been registered yet.");
                }
            }
            LOGIN.setText(null);
            PASS.setText(null);
        }
    }

    @Override
    public void viewAlert(String name, String header, String content, Alert.AlertType type) {
       Alert a = new Alert(type);
       a.setTitle(name);
       a.setHeaderText(header);
       a.setContentText(content);
       a.showAndWait();
    }

    @Override
    public String preparePath(String path) {
        return path;
    }

    @Override
    public void viewError(String text) {
        Alert a = new Alert(AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText("An error occured.");
        a.setContentText(text);
        a.showAndWait();
    } 

    private void downloadAndSetToModel() throws InterruptedException {
        Task<String> list_controller = new Task<String>(){
            @Override
            protected String call() throws AWTException, IOException, SocketException, Exception {
               TRANSPORTERS.forEach((x,y) ->{
                   try {
                       byte[] data = x.readBytes(8192);
                       Image get = processData(data);
                       VIEWER_PANEL.add(new ImageView(get), 0, 0);
                   } catch (TransportException ex) {
                       Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
                   }
               }); 
               return "";
            }

            private Image processData(byte[] buffer) {
                    OutputStream ou;
                    Image out = null;
                    ByteArrayInputStream is = null;
                    byte[] result;
                    System.out.println("Reading stream to buffer...");
                    int len = (int)buffer[0]+1;
                    char[] c = new char[len];

                    for(int i = 1; i<len; i++){
                        c[i] = (char)((int)buffer[i]);
                    }

                    String name = String.valueOf(c);
                    System.out.println("Name received: "+name);
                    System.out.println("OutputStream directed to: "+Paths.get(".").toAbsolutePath().normalize().toString()+File.separatorChar+name+".jpg");
                    //ou = new FileOutputStream(new File(getLocalVar(Local.LOCAL)+getLocalVar(Local.SEPARATOR)+name.trim()+".jpg"));
                    result = Arrays.copyOf(buffer, 8192-len);
                    try {
                        is = new ByteArrayInputStream(buffer);
                        is.read(result);
                        out = new Image(is);
                       
                    } catch (IOException e) {
                        Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, e.getLocalizedMessage());
                    } finally {
                        try{
                            if(is != null) is.close();
                        } catch (IOException ex){
                            System.err.print("No kurwa null.");
                        }
                    }

//                    try{
//                        ou.write(buffer,len,8192-len);
//                    }finally{
//                        ou.flush();
//                        ou.close();
//                        new File(Paths.get(".").toAbsolutePath().normalize().toString()+File.separatorChar+ACTUAL_NAME+".jpg").delete();
//                        System.gc();
//                        System.out.println("Downloaded.");
//                    }
            return out;
            }
        };
        Thread list_task = new Thread(list_controller,"ListUpdater");
        list_task.setDaemon(true);
        list_task.start();
      
    }

    private void initTransporters(String subnet ,int port) throws Exception {
        NetController net = new NetController();
        String[] pool = net.getIpPool(subnet);
        for(String ip : pool){
            System.out.println(ip);
            if(net.isReachable(ip)){
                Socket s = new Socket(ip,port);
                s.setTcpNoDelay(true);
                Transport tr = new Transport(s);
                BaudrateMeter bd = new BaudrateMeter();

                tr.setBaudrateMeter(bd);
                TRANSPORTERS.put(tr,bd);
            }
        }
    }
}
