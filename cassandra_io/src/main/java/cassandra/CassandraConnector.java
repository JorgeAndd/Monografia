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

	private static String CLUSTER_NAME = "2CM";
	private static int repetitions_insert = 1;
	private static int repetitions_select = 10;

	public static void insertData(String outputName) throws InterruptedException, IOException
	{   	    
    	FileWriter file = new FileWriter(outputName, true);    	
    	PrintWriter writer = new PrintWriter(file);
    	
    	writer.println("Arquivo\tRegistros\tTempo");
    	writer.flush();
    	// Lê o zip contendo os arquivos .csv
    	ZipFile zipFile = new ZipFile("dados.zip");
    	
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
	
	public static void selectData() throws IOException
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
    	reader.read(session, repetitions_select, "2CM");
    	
    	session.close();
	}
	
    public static void main( String[] args ) throws InterruptedException, RuntimeException, IOException
    {
    	String fileName = CLUSTER_NAME + "_in.csv"; 
    	
    	FileWriter file = new FileWriter("log", true);  
   	
    	FileWriter file2 = new FileWriter("log", true);  
    	PrintWriter writer = new PrintWriter(file2);
    	
    	// Insere dados no Cassandra
    	for(int i = 0; i < repetitions_insert; i++)
    	{    		
    		System.out.println("Inserindo " + i + ":\n");
    		try{
    			truncateData();
    			insertData(fileName);	
        		
        		// Na segunda execução faz os selects
        		if(i == 2)
        		{
        			selectData();
        		}
        			
        		// envia arquivo por email
        		String target = new String("/home/ppca/jorge/workspace/cassandra_in/script.sh");

                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(target);
                proc.waitFor();
    		}catch(Exception e)
    		{    	    	
    	    	writer.println(LocalDateTime.now().toString() + "\n" + e.getMessage() + "\n\n");
    	    	writer.flush();
    	    	
    	    	// Caso ocorra erro, executa novamente
    	    	i--;
    		}

    	}
    	
    	   	
    	// Envia resultados por email
		String target = new String("/home/ppca/jorge/workspace/cassandra_in/script.sh");

        Runtime rt = Runtime.getRuntime();
        Process proc = rt.exec(target);
        proc.waitFor();
    	
    	System.out.println("Finalizado");
    	return;
    }
}
