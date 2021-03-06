package hierarchicalClustering;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.apache.commons.lang3.ArrayUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.ElbowPlotter;
import com.apporiented.algorithm.clustering.visualization.DendrogramFrame;
import com.apporiented.algorithm.clustering.visualization.DendrogramPanel;

import edu.eur.absa.Framework;

public class HierarichalClusterAlgorithm {
	private Map<String, Integer> aspect_mentions = new HashMap<String, Integer>();
	private List<double[]> wordvectors = new ArrayList<double[]>(); //list with only the word vectors
	private List<String> aspects = new ArrayList<String>();
	private Map<double[], String> wordvector_aspect = new HashMap<double[], String>();
	private Map<String, double[]> aspect_wordvector = new HashMap<String, double[]>();
	private List<double[]> vector_list = new ArrayList<double[]>();
	private Map<Integer,Double> elbow_data = new HashMap<Integer, Double>();
	private Map<String,List<String>> cluster_representation = new HashMap<String,List<String>>();

	private String[] terms;
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	private Map<String, Map<double[], double[]>> finalMap = new HashMap<>(); 

	public HierarichalClusterAlgorithm(String filelocation_yelp, String filelocation_aspects, String[] terms) throws ClassNotFoundException, IOException {
		this.terms = terms;
		read_file_wordvec("finalMap", filelocation_yelp);
		for(Map.Entry<String, Map<double[], double[]>> entry : finalMap.entrySet()) {
			Map.Entry<double[], double[]> entry2 = finalMap.get(entry.getKey()).entrySet().iterator().next(); 
			word_vec_yelp.put(entry.getKey(), entry2.getKey()); 
		}
		read_file(filelocation_aspects);
		create_aspect_wordvec();
		create_wordvec_aspect();

	}

	public Map<String,List<String>> getClusterRepresentation() {
		return cluster_representation;
	}

	public void read_file_wordvec(String dataset, String filelocation) throws IOException, ClassNotFoundException {
		if (dataset == "finalMap") {
			File toRead_yelp=new File(filelocation);
			FileInputStream fis_yelp=new FileInputStream(toRead_yelp);
			ObjectInputStream ois_yelp =new ObjectInputStream(fis_yelp);
			finalMap =(HashMap<String,Map<double[], double[]>>)ois_yelp.readObject();
			ois_yelp.close();
			fis_yelp.close();	
		}
	}


	public void read_file(String filelocation) throws IOException, ClassNotFoundException {		
		File toRead_sent=new File(filelocation);
		FileInputStream fis_sent=new FileInputStream(toRead_sent);
		ObjectInputStream ois_sent =new ObjectInputStream(fis_sent);
		aspect_mentions =(HashMap<String,Integer>)ois_sent.readObject();
		ois_sent.close();
		fis_sent.close();	
	}



	public void create_aspect_wordvec() {
		for (String term : terms) {
			if (word_vec_yelp.get(term) != null) {
				aspect_wordvector.put(term, word_vec_yelp.get(term));
			}
			else {
				terms = ArrayUtils.removeElement(terms, term);
			}
		}
	}

	public void create_wordvec_aspect()	{
		for (String term: terms) {
			if (word_vec_yelp.get(term) != null) {
				wordvector_aspect.put(word_vec_yelp.get(term), term);
			}
			else {
				terms = ArrayUtils.removeElement(terms, term);
			}
		}
	}



	/*
	 * for (Map.Entry<String, Integer> entry: aspect_mentions.entrySet()) { if
	 * (word_vec_yelp.get(entry.getKey()) != null) {
	 * aspect_wordvector.put(entry.getKey(), word_vec_yelp.get(entry.getKey()));
	 * } }
	 */
	/*
	public void create_wordvec_aspect() {
		for (Map.Entry<String, Integer> entry: aspect_mentions.entrySet()) {
			if (word_vec_yelp.get(entry.getKey()) != null) {
				wordvector_aspect.put(word_vec_yelp.get(entry.getKey()), entry.getKey());
			}
		}
	}*/


	public double get_cosine_similarity(double[] vec1, double[] vec2) {	
		return (dotProduct(vec1, vec2)/(getMagnitude(vec2) * getMagnitude(vec1)));
	}


	public int recursion_depth(Cluster cluster) {
		if (!cluster.isLeaf()){
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				return recursion_depth(cluster) + 1;
			}
			if (child_clusters.size() == 2) {
				return Math.max(recursion_depth(child_clusters.get(0)), recursion_depth(child_clusters.get(1))) + 1;
			}
		}

		return 0;
	}

	public List<double[]> get_vector_list(Cluster cluster, List<double[]> vector_list){
		if (cluster.isLeaf() && aspect_wordvector.get(cluster.getName()) != null){
			vector_list.add(aspect_wordvector.get(cluster.getName()));		
		}
		else {	
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				get_vector_list(child_clusters.get(0), vector_list);
			}
			if (child_clusters.size() == 2) {
				get_vector_list(child_clusters.get(0), vector_list);
				get_vector_list(child_clusters.get(1), vector_list);
			}
		}
		return vector_list;
	}


	public double calculate_WSS(Cluster cluster) { 
		List<double[]> vector_list = new ArrayList<double[]>();
		List<double[]> vectors_list = get_vector_list(cluster, vector_list);
		double [] average_vector;

		//if (!vectors_list.isEmpty())
		//{
			average_vector= new double[vectors_list.get(0).length];
		

		for( int i = 0; i<vectors_list.size(); i++) { 
			for (int j = 0; j < vectors_list.get(0).length; j ++) {
				average_vector[j] += vectors_list.get(i)[j];
				if (i == vectors_list.size() - 1) {
					average_vector[j] = average_vector[j] / (double)vectors_list.size();
				}
			}
		}
		double WSS = 0;
		for (int x = 0; x < vectors_list.size(); x++) {
			for (int z = 0; z < vectors_list.get(x).length; z ++) {
				WSS += Math.pow(vectors_list.get(x)[z]-average_vector[z], 2);
			}
		}
		return WSS;
		//}
		
		/*
		else
		{
			return 0; 
		}
		*/
		
	}


	public double get_WSS(int total_depth, int max_depth, Cluster cluster) {
		if (total_depth < max_depth) {
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				total_depth += 1;
				return get_WSS(total_depth, max_depth, child_clusters.get(0));
			}
			if (child_clusters.size() == 2) {
				total_depth += 1;
				return get_WSS(total_depth, max_depth, child_clusters.get(0)) + get_WSS(total_depth, max_depth, child_clusters.get(1));	}

		}

		return calculate_WSS(cluster);
	}

	public void elbow_method(int total_depth, Cluster cluster) {
		for (int i = 0; i < total_depth; i++) {
			double WSS = get_WSS(0,i, cluster);
			elbow_data.put(i, WSS);


		}
	}


	public void make_plot() {

		final ElbowPlotter demo = new ElbowPlotter("Elbow method", elbow_data );
		demo.pack();
		demo.setVisible(true);

	}

	public String get_maximum_average_similarity(List<double[]> vector_list) {
		double[] max_vector = vector_list.get(0);
		double max_average_similarity = 0;
		for (double[] base_vector: vector_list) {
			double average_similarity = 0;
			int count = 0;
			for (double[] to_compare: vector_list) {
				if (!base_vector.equals(to_compare)){
					count += 1;
					average_similarity += get_cosine_similarity(base_vector, to_compare);

				}
			}
			average_similarity = average_similarity / count;
			if(average_similarity > max_average_similarity) {
				max_average_similarity = average_similarity;
				max_vector = base_vector;
			}
		}


		return wordvector_aspect.get(max_vector);
	}


	public void rename_subclusters(int cut_off_depth, int current_depth, Cluster cluster) {
		if (!cluster.isLeaf()) {
			List<double[]> vector_list = new ArrayList<double[]>();
			List<double[]> vectors_list = get_vector_list(cluster, vector_list);
			String new_name = get_maximum_average_similarity(vectors_list);
			cluster.setName(new_name);

			if (current_depth < cut_off_depth) {
				List<Cluster> child_clusters = cluster.getChildren();
				if (child_clusters.size() == 1) {
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(0));
				}
				if (child_clusters.size() == 2) {
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(0));
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(1));
				}
			}
		}	
	}


	public void addToMap(String mapKey, String word_to_add) {
		List<String> itemsList = cluster_representation.get(mapKey);

		// if list does not exist create it
		if(itemsList == null)  {
			itemsList = new ArrayList<String>(); 
			itemsList.add(word_to_add);
			cluster_representation.put(mapKey, itemsList); 
		}     
		else 
		{ // add if item is not already in list
			if(!itemsList.contains(word_to_add)) 
				itemsList.add(word_to_add); 
		} 
	}


	public void leaf_node_handler(Cluster original_cluster, Cluster cluster) {
		if (!cluster.isLeaf()) {
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				leaf_node_handler(original_cluster, child_clusters.get(0));
			}
			if (child_clusters.size() == 2) {
				leaf_node_handler(original_cluster, child_clusters.get(0));
				leaf_node_handler(original_cluster, child_clusters.get(1));
			}

		}
		else {
			addToMap(original_cluster.getName(), cluster.getName());
		}

	}

	public void create_cluster_representation(Cluster cluster, int current_depth, int cut_off_depth) {
		if (!cluster.isLeaf()) {
			List<String> child_names = new ArrayList<String>();
			List<Cluster> child_clusters = cluster.getChildren();
			for (Cluster child: child_clusters) {
				child_names.add(child.getName());
			}
			cluster_representation.put(cluster.getName(), child_names);
			if (current_depth < cut_off_depth) {
				if (child_clusters.size() == 1) {
					create_cluster_representation(child_clusters.get(0), current_depth + 1, cut_off_depth);
				}
				if (child_clusters.size() == 2) {
					create_cluster_representation(child_clusters.get(0), current_depth + 1, cut_off_depth);
					create_cluster_representation(child_clusters.get(1), current_depth + 1, cut_off_depth);
				}
			}
			else {
				leaf_node_handler(cluster, cluster);
			}
		}
	}

	public void parent_child_printer(Cluster cluster, int current_depth, int cut_off_depth) {
		if (cluster.isLeaf()) {
			System.out.println("This is a leaf node, name: " + cluster.getName());
		}
		else {
			System.out.println("Parent: " + cluster.getName() + ", Children: " + cluster.getChildren());
		}
		if (current_depth < cut_off_depth) {
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				parent_child_printer(child_clusters.get(0) , current_depth+1, cut_off_depth);
			}
			if (child_clusters.size() == 2) {
				parent_child_printer(child_clusters.get(0) , current_depth+1, cut_off_depth);
				parent_child_printer(child_clusters.get(1) , current_depth+1, cut_off_depth);
			}
		}
	}

	public static double getDistance(double[] term1, double[] term2) {
		double distance = 0;
		for (int i = 0; i< term1.length; i++){
			distance += Math.pow(term1[i]-term2[i],2);
		}
		return Math.sqrt(distance);
	}


	public static double[][] getDistanceMatrix(String[] terms, Map<String, double[]> aspect_wordvector) {
		int totalterms = terms.length;
		double[][] distancematrix = new double[totalterms][totalterms];
		for (int i = 0; i < totalterms; i++) {
			for (int j = 0; j < totalterms; j++) {
				double[] rowterm = aspect_wordvector.get(terms[i]);
				double[] columnterm = aspect_wordvector.get(terms[j]);
				double distance = getDistance(rowterm, columnterm);
				distancematrix[i][j] = distance;
			}
		}
		return distancematrix;
	}



	public static void main(String[] args) throws ClassNotFoundException, IOException {
		String[] mentionclasses = {"staff", "communication", "communications", "experiences", "interactions", "interaction", "attitude", "attitudes", "waitstaff", "services"};
		HierarichalClusterAlgorithm test = new HierarichalClusterAlgorithm(Framework.OUTPUT_PATH + "finalMap2",  Framework.OUTPUT_PATH + "aspect_mentions", mentionclasses);
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();

		String[] terms = test.terms;
		double[][] distances = getDistanceMatrix(test.terms, test.aspect_wordvector);
		String[] names = new String[terms.length];

		for(int i = 0; i < terms.length; i++) {
			names[i] = String.valueOf(i);
		}

		Cluster cluster = alg.performClustering(distances, terms, new AverageLinkageStrategy());
		int recursion = test.recursion_depth(cluster);
		test.rename_subclusters(1, 0, cluster);
		test.create_cluster_representation(cluster, 0, 1);
		test.parent_child_printer(cluster, 0, 3);
		System.out.println(test.cluster_representation);
		test.elbow_method(recursion, cluster );
		test.make_plot();
		Frame f1 = new DendrogramFrame(cluster);
		f1.setSize(500, 400);
		f1.setLocation(100, 200);
		test.make_plot();


	}


	public static double dotProduct(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}
	public static double getMagnitude(double[] vec_1) {
		double magnitude = 0;
		for (int j = 0; j < vec_1.length; j++) {
			magnitude += Math.pow(vec_1[j],2);
		}
		return Math.sqrt(magnitude);
	}

}