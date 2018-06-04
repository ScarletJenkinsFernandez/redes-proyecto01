package redesbriar;

import java.nio.charset.Charset;
import java.util.Arrays;

public class RedesBriar {

    public static void main(String[] args) {
        Converter conv = Converter.getInstance();
        
        /* Mensaje para escribir */
        //conv.setMessage("Hello World!");
        
        /* Ruta de archivo para leer */
        conv.setReadFilePath("C:\\Users\\Scarlet\\Documents\\sCa\\TEC\\Semestre I 2018\\Redes\\Proyecto\\");
        
        /* Ruta de archivo para ecribir */
        //conv.setWriteFilePath("C:\\Users\\Scarlet\\Documents\\sCa\\TEC\\Semestre I 2018\\Redes\\Proyecto\\");
        
        /* Nombre archivo para leer */
        conv.setReadFileName("test.wav");
        
        /* Nombre archivo para ecribir */
        //conv.setReadFileName("test.wav");
        
        /* Escribe */
        //conv.writeWAVFile();
        //conv.printInfo();
        
        /* Lee */
        conv.readWAVFile();
        conv.printInfo();
    }
    
}
