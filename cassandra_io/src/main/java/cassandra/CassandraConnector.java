package cassandra;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;

public class CassandraConnector{

	private static String CLUSTER_NAME = "6CM";
	private static String CLUSTER_FILE_NAME;
	private static int repetitions_insert = 10;
	private static int repetitions_select = 10;

	public static void insertData(String inputName, String outputName) throws InterruptedException, IOException
	{   	    
    	FileWriter file = new FileWriter(outputName, true);    	
    	PrintWriter writer = new PrintWriter(file);
    	
    	writer.println("Arquivo\tRegistros\tTempo");
    	writer.flush();
    	// Lê o zip contendo os arquivos .csv
    	ZipFile zipFile = new ZipFile(inputName + ".zip");
    	
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        InputStream stream = null;
        
        
        // Itera pelos arquivos presentes no zip

        while(entries.hasMoreElements()){
            ZipEntry entry = entries.nextElement();
            stream = zipFile.getInputStream(entry);
            
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			final String insertCQL = "INSERT INTO bolsa_familia.dados "
					+ "(uf, cod_municipio, nome_municipio, nis_favorecido, nome_favorecido, fonte, valor, periodo) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, ?)";
			
			long start = System.nanoTime();
			
			System.out.println("Lendo conteudo do arquivo " + entry.getName());
	    	
			long registros = new CassandraLoader(6, "127.0.0.1").insert(br, insertCQL);
	    	
	    	double elapsed = (double)((System.nanoTime() - start) / 1000000000.0);
	        System.out.println("Tempo total(segundos): " + elapsed);
	        System.out.println("Registros inseridos: " + registros);
	        System.out.println(registros / elapsed + " registros/segundo\n");
			
	        writer.println(entry.getName() + "\t" + registros + "\t" + elapsed);
	        writer.flush();
        }
        
        writer.println();
        
        stream.close();
        zipFile.close();
		writer.close();
	}
	
	public static void truncateData()
	{
		String serverIP = "localhost";
		
		Cluster cluster = Cluster.builder()
		    	  .addContactPoints(serverIP)
		          .withSocketOptions(
		                  new SocketOptions()
		                          .setConnectTimeoutMillis(600000)
		                          .setReadTimeoutMillis(600000))
		    	  .build();
		
		Session session = cluster.connect("bolsa_familia");
		String query = "TRUNCATE dados";
		session.execute(query);
		System.out.println("Table truncated");
	}
	
	public static void selectData() throws Exception
	{
		String serverIP = "localhost";
		Cluster cluster = Cluster.builder()
		    	  .addContactPoints(serverIP)
		          .withSocketOptions(
		                  new SocketOptions()
		                          .setConnectTimeoutMillis(600000)
		                          .setReadTimeoutMillis(600000))
		    	  .build();
		
		Session session = cluster.connect("bolsa_familia");
    	
		// Le os dados
		System.out.println("Lendo dados...\n");
		CassandraReader reader = new CassandraReader();
    	reader.read(session, repetitions_select, CLUSTER_FILE_NAME);
    	
    	session.close();
	}
	
    public static void main( String[] args ) throws InterruptedException, RuntimeException, IOException
    {    	
    	
    	// String[] zipFiles = new String[] { "dados", "dados_1ano", "dados_6meses" };
    	String[] zipFiles = new String[] { "dados_1ano", "dados_6meses" };
    	
    	for(String zip : zipFiles)
    	{
    		CLUSTER_FILE_NAME = CLUSTER_NAME + "_" + zip; 
        	String fileName =  CLUSTER_FILE_NAME + "_in.csv"; 
    	  	
        	FileWriter file = new FileWriter("log", true);  
        	PrintWriter writer = new PrintWriter(file);
    		
	    	// Insere dados no Cassandra
	    	for(int i = 0; i < repetitions_insert; i++)
	    	{    		
	    		System.out.println("Inserindo " + i + ":\n");
	    		try{
	    			truncateData();
	    			insertData(zip, fileName);	
	        		
	        		// Depois da primeira inserção faz o select
	        		if(i == 0 && repetitions_select > 0)
	        		{
	        			boolean done = false;
	            		
	            		while(!done)
	            		{
	            			try{
	            				selectData();
	            				done = true;
	            			}
	            			catch(Exception e)
	            			{
	                	    	writer.println(LocalDateTime.now().toString() + "\n" + e.getMessage() + "\n\n");
	                	    	System.out.print(LocalDateTime.now().toString() + "\n" + e.getMessage() + "\n\n");
	                	    	writer.flush();
	            				done = false;
	            			}	
	            		}
	        		}      		
	        		
	    		}catch(Exception e)
	    		{    	    	
	    	    	writer.println(LocalDateTime.now().toString() + "\n" + e.getMessage() + "\n\n");
	    	    	System.out.print(LocalDateTime.now().toString() + "\n" + e.getMessage() + "\n\n");
	    	    	writer.flush();
	    	    	
	    	    	// Caso ocorra erro, executa novamente
	    	    	truncateData();
	    	    	i--;
	    		}
	
	    	}
	    	truncateData();
    	}
    	   	
    	// Envia resultados por email
		String target = new String("/home/bioinformatica/jorge/workspace/cassandra_io/script.sh");

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(target);
        proc.waitFor();
    	
    	System.out.println("Finalizado");
    	return;
    }
}
