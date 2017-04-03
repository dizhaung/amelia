/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package abstracts;

import com.neology.net.NetController;
import com.neology.xml.XMLController;
import enums.Local;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Obsidiam
 */
public abstract class LocalEnvironment {
    private NetController NET = new NetController();
   
    
    public String getLocalVar(Local l) throws NullPointerException{
        String var = null;
        XMLController xml = new XMLController();
        
        switch(l){
            case LOCAL:
                var = Paths.get(".").toAbsolutePath().normalize().toString();
                break;
            case USER_NAME:
                var = System.getProperty("user.name");
                break;
            case OS:
                var = System.getProperty("os.name");
                break;
            case SEPARATOR:
                var = File.separator;
                break;
            case SUBNET:{
                try {
                    var = xml.parseInitFile().get(0).toString();
                } catch (SAXException | IOException | ParserConfigurationException ex) {
                    Logger.getLogger(LocalEnvironment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
                break;
            case USER_HOME:
                var = System.getProperty("user.home");
                break;
            case XML_PATH:{
                try {
                    if(new File("init.xml").exists()){
                        var = xml.parseInitFile().get(1).toString();
                    }else{
                        var = Paths.get(".").toAbsolutePath().normalize().toString(); 
                    }
                    
                } catch (SAXException | IOException | ParserConfigurationException ex) {
                    Logger.getLogger(LocalEnvironment.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            break;
        }
       return var;
    }
    
    public abstract String preparePath(String path);
    
}