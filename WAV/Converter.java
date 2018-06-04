package redesbriar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Converter { 
    /*
    http://soundfile.sapp.org/doc/WaveFormat/
    The canonical WAVE file format
   ------------------------------------------------------------------------------------------
    endian | offset  | size  | nombre        |  descripción
           | (bytes) |(bytes)|               |
   ------------------------------------------------------------------------------------------
    big    | 0       | 4     | ChunkID       | "RIFF"
   ------------------------------------------------------------------------------------------
    little | 4       | 4     | ChunkSize     | Tamaño del archivo - 8 (ChunkID, ChunkSize)
   ----------------------------------------------------------------
    big    | 8       | 4     | Format        | "WAVE" (tiene 2 subchunks: "fmt " y "data")
   ------------------------------------------------------------------------------------------
    "fmt ": describe el formato de los datos de sonido:
   ------------------------------------------------------------------------------------------
    big    | 12      | 4     | Subchunk1ID   | "fmt "
   ------------------------------------------------------------------------------------------                        
    little | 16      | 4     | Subchunk1Size | 16 PCM
   ------------------------------------------------------------------------------------------
    little | 20      | 2     | AudioFormat   | PCM = 1 
   ------------------------------------------------------------------------------------------
    little | 22      | 2     | NumChannels   | Mono = 1, Stereo = 2
   ------------------------------------------------------------------------------------------
    little | 24      | 4     | SampleRate    | 8000, 44100
   ------------------------------------------------------------------------------------------
    little | 28      | 4     | ByteRate      | == SampleRate * NumChannels * BitsPerSample/8
   ------------------------------------------------------------------------------------------
    little | 32      | 2     | BlockAlign    | == NumChannels * BitsPerSample/8
   ------------------------------------------------------------------------------------------                               
    little | 34      | 2     | BitsPerSample | 8 bits = 8, 16 bits = 16
   ------------------------------------------------------------------------------------------  
    "data": contiene el tamaño de los datos y el sonido:
   ------------------------------------------------------------------------------------------  
    big    | 36      | 4     | Subchunk2ID   | "data"
   ------------------------------------------------------------------------------------------ 
    little | 40      | 4     | Subchunk2Size | == NumSamples * NumChannels * BitsPerSample/8
   ------------------------------------------------------------------------------------------
    little | 44      | *     | Data          | Los datos de sonido
   ------------------------------------------------------------------------------------------
    */
  
    /* WAV */
    private int CHUNK_SIZE                 = 36; //36 + SUB_CHUNK_2_SIZE;
    private int SUB_CHUNK_1_SIZE           = 16;
    private short AUDIO_FORMAT             = 1;
    private int NUMBER_OF_CHANNELS         = 1;
    private int SAMPLE_RATE                = 48000;                                                   
    private int BITS_PER_SAMPLE            = 16; 
    private short SHORT_NUMBER_OF_CHANNELS = (short) NUMBER_OF_CHANNELS;
    private short SHORT_BITS_PER_SAMPLE    = (short) BITS_PER_SAMPLE;
    private int BYTE_RATE                  = SAMPLE_RATE * NUMBER_OF_CHANNELS * BITS_PER_SAMPLE / 8;
    private short BLOCK_ALIGN              = (short) (NUMBER_OF_CHANNELS * BITS_PER_SAMPLE / 8);
    private int SUB_CHUNK_2_SIZE           = 0; //numberOfSamples * NUMBER_OF_CHANNELS * BITS_PER_SAMPLE / 8;
    private byte[] DATA;
 
    private String DATA_CHUNK_ID = "";
    private String SUB_CHUNK_1_ID = "";
    private String FORMAT = "";
    private String CHUNK_ID = "";
    private int DATA_SIZE = 0;

    /* ARCHIVO */
    private String READ_FILE_PATH;
    private String WRITE_FILE_PATH;
    private String READ_FILE_NAME;
    private String WRITE_FILE_NAME;
    
    /* BUFFERS */
    private ByteBuffer BYTE_BUFFER_INT;
    private ByteBuffer BYTE_BUFFER_SHORT;
    
    /* MENSAJE */
    private String MESSAGE = "";

    
    /**
     * Singleton
     */
    private static Converter instance = null;
    
    public static Converter getInstance() {
	if (instance == null) {
            instance = new Converter();
        }
        return instance;
    }
    
    /**
     * SETs
     */
    public void setMessage(String msg) {
        this.MESSAGE = msg;
        this.DATA = messageToByte(msg);
        this.SUB_CHUNK_2_SIZE = this.DATA.length;
        this.CHUNK_SIZE += this.SUB_CHUNK_2_SIZE;
    }
    
    public void setReadFilePath(String path) {
        this.READ_FILE_PATH = path;
    }
    
    public void setWriteFilePath(String path) {
        this.WRITE_FILE_PATH = path;
    }
    
    public void setReadFileName(String name) {
        this.READ_FILE_NAME = name;
    }
    
    public void setWriteFileName(String name) {
        this.WRITE_FILE_NAME = name;
    }
    
    /**
     * Convierte de int/short a un arreglo de bytes
     * @param i : int/short
     * @param endian : true = little endian; false = big endian
     */
    private byte[] intToByte(int i, boolean endian) {
        BYTE_BUFFER_INT = ByteBuffer.allocate(4);
        if (endian) {
            BYTE_BUFFER_INT.order(ByteOrder.LITTLE_ENDIAN);
            return BYTE_BUFFER_INT.putInt(i).array();
        }
        BYTE_BUFFER_INT.order(ByteOrder.BIG_ENDIAN);
        return BYTE_BUFFER_INT.putInt(i).array();
    }
    
    private byte[] shortToByte(short i, boolean endian) {
        BYTE_BUFFER_SHORT = ByteBuffer.allocate(2);
        if (endian) {
            BYTE_BUFFER_SHORT.order(ByteOrder.LITTLE_ENDIAN);
            return BYTE_BUFFER_SHORT.putShort(i).array();
        }
        BYTE_BUFFER_SHORT.order(ByteOrder.BIG_ENDIAN);
        return BYTE_BUFFER_SHORT.putShort(i).array();
    }
    
    /**
     * Convierte de un arreglo de bytes a int/short
     * @param b : arreglo de bytes
     * @param endian : true = little endian; false = big endian
     */
    private int byteToInt(byte[] b, boolean endian) {
        if (endian) {
            return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt();
    }
    
    private short byteToShort(byte[] b, boolean endian) {
        if (endian) {
            return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getShort();
    }
    
    /**
     * Convierte de mensaje a arreglo de bytes
     * @param msg : mensaje
     */
    private byte[] messageToByte(String msg) {
        return msg.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convierte de arreglo de bytes a mensaje
     * @param b : arreglo de bytes
     */
    private String byteToMessage(byte[] b) {
        return new String(b);
    }
  
    /**
     * Lee archivo .wav
     */
    public void readWAVFile() {   
        try {        
            byte[] file = Files.readAllBytes(Paths.get(READ_FILE_PATH + READ_FILE_NAME));
            byte[] tmp = null;
            tmp = Arrays.copyOfRange(file, 0, 4);                  // 00 - RIFF
            CHUNK_ID = Arrays.toString(tmp);
            tmp = Arrays.copyOfRange(file, 4, 8);                  // 04 - tamaño de aquí en adelante
            CHUNK_SIZE = byteToInt(tmp, true);
            tmp = Arrays.copyOfRange(file, 8, 12);                 // 08 - WAVE
            FORMAT = Arrays.toString(tmp);       
            tmp = Arrays.copyOfRange(file, 12, 16);                // 12 - fmt
            SUB_CHUNK_1_ID = Arrays.toString(tmp);
            tmp = Arrays.copyOfRange(file, 16, 20);                // 16 - tamaño de aquí en adelante
            SUB_CHUNK_1_SIZE = byteToInt(tmp, true);
            tmp = Arrays.copyOfRange(file, 20, 22);                // 20 - formato de audio 1
            AUDIO_FORMAT = byteToShort(tmp, true);
            tmp = Arrays.copyOfRange(file, 22, 24);                // 22 - mono 1 o stereo 2 
            SHORT_NUMBER_OF_CHANNELS = byteToShort(tmp, true);
            tmp = Arrays.copyOfRange(file, 24, 28);                // 24 - muestras por segundo
            SAMPLE_RATE = byteToInt(tmp, true);
            tmp = Arrays.copyOfRange(file, 28, 32);                // 28 - bytes por segundo
            BYTE_RATE = byteToInt(tmp, true);
            tmp = Arrays.copyOfRange(file, 32, 34);                // 32 - # de bytes en una muestra, todos los canales
            BLOCK_ALIGN = byteToShort(tmp, true);
            tmp = Arrays.copyOfRange(file, 34, 36);                // 34 - bits por muestra (16 or 24)
            SHORT_BITS_PER_SAMPLE = byteToShort(tmp, true);
            tmp = Arrays.copyOfRange(file, 36, 40);                // 36 - data
            DATA_CHUNK_ID = Arrays.toString(tmp);
            tmp = Arrays.copyOfRange(file, 40, 44);                // 40 - tamaño de aquí en adelante
            DATA_SIZE = byteToInt(tmp, true);  
            tmp = Arrays.copyOfRange(file, 44, 44 + DATA_SIZE);    // 44 - info (data)                 
            DATA = tmp;
            MESSAGE = byteToMessage(DATA);  
	}
	catch(Exception e) {
            System.out.println(e.getMessage());
	}
    }
    
    /**
     * Escribe archivo .wav
     */
    public void writeWAVFile() {
        try {          
            DataOutputStream outFile  = new DataOutputStream(new FileOutputStream(WRITE_FILE_PATH + WRITE_FILE_NAME));
            outFile.writeBytes("RIFF");					      // 00 - RIFF
            outFile.write(intToByte(CHUNK_SIZE, true), 0, 4);	              // 04 - tamaño de aquí en adelante
            outFile.writeBytes("WAVE");					      // 08 - WAVE
            outFile.writeBytes("fmt ");					      // 12 - fmt 
            outFile.write(intToByte(SUB_CHUNK_1_SIZE, true), 0, 4);           // 16 - tamaño de aquí en adelante
            outFile.write(shortToByte(AUDIO_FORMAT, true), 0, 2);	      // 20 - formato de audio 1 -> PCM = Pulse Code Modulation
            outFile.write(shortToByte(SHORT_NUMBER_OF_CHANNELS, true), 0, 2); // 22 - mono 1 o stereo 2
            outFile.write(intToByte(SAMPLE_RATE, true), 0, 4);	              // 24 - muestras por segundo 
            outFile.write(intToByte(BYTE_RATE, true), 0, 4);	              // 28 - bytes por segundo
            outFile.write(shortToByte(BLOCK_ALIGN, true), 0, 2);	      // 32 - # de bytes en una muestra, todos los canales
            outFile.write(shortToByte(SHORT_BITS_PER_SAMPLE, true), 0, 2);    // 34 - bits por muestra (16 or 24)
            outFile.writeBytes("data");					      // 36 - data
            outFile.write(intToByte(SUB_CHUNK_2_SIZE, true), 0, 4);	      // 40 - tamaño de aquí en adelante
            outFile.write(DATA);					      // 44 - info (data)
            
            outFile.close();           
	} catch(IOException e) {
            System.out.println(e.getMessage());
	} 
    }
    
    /**
     * Imprime variables (DEBUG)
     */
    public void printInfo() {       
        System.out.println("CHUNK_ID: " + CHUNK_ID);
        System.out.println("CHUNK_SIZE: " + CHUNK_SIZE);
        System.out.println("FORMAT: " + FORMAT);
        System.out.println("SUB_CHUNK_1_ID: " + SUB_CHUNK_1_ID);
        System.out.println("SUB_CHUNK_1_SIZE: " + SUB_CHUNK_1_SIZE);
        System.out.println("AUDIO_FORMAT: " + AUDIO_FORMAT);
        System.out.println("SHORT_NUMBER_OF_CHANNELS: " + SHORT_NUMBER_OF_CHANNELS);
        System.out.println("SAMPLE_RATE: " + SAMPLE_RATE);
        System.out.println("BYTE_RATE: " + BYTE_RATE);
        System.out.println("BLOCK_ALIGN: " + BLOCK_ALIGN);
        System.out.println("SHORT_BITS_PER_SAMPLE: " + SHORT_BITS_PER_SAMPLE);
        System.out.println("DATA_CHUNK_ID: " + DATA_CHUNK_ID);
        System.out.println("DATA_SIZE: " + DATA_SIZE);
        System.out.println("DATA: " + DATA);
        System.out.println("MESSAGE(BYTES): " + Arrays.toString(DATA));
        System.out.println("MESSAGE: " + MESSAGE);                  
    }
}
