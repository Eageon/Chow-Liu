import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;


public class ChowLiu {
	
	GraphicalModel model;
	GraphicalModel origModel;
	
	ArrayList<MutualInfo> pairs;
	LinkedList<Evidence> evidenceSet;
	int numEvidence;
	
	public class MutualInfo implements Comparable<MutualInfo> {
		ArrayList<Variable> varPair = new ArrayList<>(2);
		double mutualInfo;
		@Override
		public int compareTo(MutualInfo o) {
			// TODO Auto-generated method stub
			return Double.compare(this.mutualInfo, o.mutualInfo);
		}
		
		public MutualInfo(Variable var1, Variable var2) {
			varPair.add(var1);
			varPair.add(var2);
		}
		
		public Variable from() {
			return varPair.get(0);
		}
		
		public Variable to() {
			return varPair.get(1);
		}
		
		/**
		 * 
		 * @param index
		 *            of table
		 * @return the set of the values of correspending variables
		 */
		public int[] tableIndexToVaraibleValue(int index) {
			int[] values = new int[2];

			int denum = varPair.get(0).domainSize() * varPair.get(1).domainSize();

			for (int i = 0; i < values.length; i++) {
				denum /= varPair.get(i).domainSize();
				int trueValue = index / denum;
				index %= denum;
				values[i] = trueValue;
			}

			return values;
		}

		/**
		 * do reversely with tableIndexToVaraibleValue
		 * 
		 * @param values
		 *            the indices of values of variables
		 * @return the index of table
		 */
		public int variableValueToTableIndex(int[] values) {
			int multi = 1;
			int index = 0;

			for (int i = values.length - 1; i >= 0; i--) {
				index += values[i] * multi;
				multi *= varPair.get(i).domainSize();
			}

			return index;
		}
		
		public int tableSize() {
			return varPair.get(0).domainSize() * varPair.get(1).domainSize();
		}
		
		public void setValues(int[] values) {
			varPair.get(0).setSoftEvidence(values[0]);
			varPair.get(1).setSoftEvidence(values[1]);
		}
	}
	
	
	public ArrayList<MutualInfo> initMutualInfoPair() {
		int n = model.variables.size();
		pairs = new ArrayList<>((n * n - n) / 2);
		
		for (int i = 0; i < model.variables.size(); i++) {
			for (int j = i + 1; j < model.variables.size(); j++) {
				MutualInfo mutualInfo = new MutualInfo(model.getVariable(i), model.getVariable(j));
				pairs.add(mutualInfo);
			}
		}
		
		return pairs;
	}
	
	public ArrayList<MutualInfo> computeMutualInfo() {
		for (int i = 0; i < pairs.size(); i++) {
			MutualInfo mutualInfo = pairs.get(i);
			
			for (int j = 0; j < mutualInfo.tableSize(); j++) {
				int[] values = mutualInfo.tableIndexToVaraibleValue(j);
				
				mutualInfo.setValues(values);
				
				int numXiXj = 0;
				int numXi = 0;
				int numXj = 0;

				for (Evidence e : evidenceSet) {
					if (e.isConsistentWith(mutualInfo.varPair, values)) {
						numXiXj++;
					}
					if (e.isConsistentWith(mutualInfo.varPair.get(0), values[0])) {
						numXi++;
					}
					if (e.isConsistentWith(mutualInfo.varPair.get(1), values[1])) {
						numXj++;
					}
				}

				if (numXi == 0 || numXj == 0) {
					mutualInfo.mutualInfo += 0.0;
					continue;
				}

				double PrD_Xi_and_Xj = (double) numXiXj / evidenceSet.size();
				double PrD_Xi = (double) numXi / evidenceSet.size();
				double PrD_Xj = (double) numXj / evidenceSet.size();
				mutualInfo.mutualInfo += PrD_Xi_and_Xj * (Math.log(PrD_Xi_and_Xj / (PrD_Xi * PrD_Xj)) / Math.log(2));

			}
		}
		
		return pairs;
	}
	
	public ArrayList<MutualInfo> sortByDescendOrder() {
		Collections.sort(pairs);
		Collections.reverse(pairs);
		
		return pairs;
	}
	
	public GraphicalModel generateChowLiuFactor() {
		for (int i = 0; i < pairs.size(); i++) {
			MutualInfo mutualInfo = pairs.get(i);
			
			if(pathExists(mutualInfo.from(), mutualInfo.to())) {
				continue;
			}
			
			model.factors.get(mutualInfo.to().index).variables.add(0, mutualInfo.from());
		}
		
		return model;
	}
	
	public boolean pathExists(Variable from, Variable to) {
		Factor fromFactor = model.getFactor(from.index);
		//Factor toFactor = model.getFactor(to.index);
		
		if (from == to) {
			return true;
		}
		
		if(fromFactor.variables.contains(to)) {
			return true;
		}
		
		for (int i = 0; i < fromFactor.numScopes() - 1; i++) {
			if(pathExists(fromFactor.variables.get(i), to)) {
				return true;
			}
		}
		
		return false;
	}
	
	public void readEvidenceSet(BufferedReader reader) {
		evidenceSet = new LinkedList<>();

		String line = null;
		try {
			while (null != (line = reader.readLine())) {
				String[] observed = line.split(" ");
				Evidence evidence = new Evidence(model.variables);
				evidence.setData(observed);
				evidenceSet.add(evidence);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readTrainingDataOnFile(String training_data) {
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(training_data));
			String preamble = reader.readLine();
			String[] tokens = preamble.split(" ");

			if (model.variables.size() != Integer.valueOf(tokens[0])) {
				System.out
						.println("uai and training data don't match on number of variables");
				System.exit(0);
			}

			numEvidence = Integer.valueOf(tokens[1]);
			readEvidenceSet(reader);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpNetworkAsUAI(String output_uai) {
		PrintStream writer = null;
		try {
			writer = new PrintStream(output_uai);
			writer.println(model.network);
			// number of variables
			writer.println(model.variables.size());
			// domain size of variables
			for (int i = 0; i < model.variables.size(); i++) {
				writer.print(model.getVariable(i).domainSize());
				if (i != model.variables.size() - 1) {
					writer.print(" ");
				}
			}
			writer.println();

			// number of factors
			writer.println(model.factors.size());
			// scope of factors
			for (int i = 0; i < model.factors.size(); i++) {
				Factor factor = model.getFactor(i);
				for (int j = 0; j < factor.variables.size(); j++) {
					writer.print(factor.getVariable(j).index);
					if (i != factor.variables.size() - 1) {
						writer.print(" ");
					}
				}
				writer.println();
			}
			writer.println();

			DecimalFormat roundFormat = new DecimalFormat("#.########");
			// CPTs
			for (int i = 0; i < model.factors.size(); i++) {
				Factor factor = model.getFactor(i);
				int domainSize = factor.getNodeVariable().domainSize();
				writer.println(roundFormat.format(factor.table.size()));
				for (int j = 0; j < factor.table.size(); j++) {
					writer.print(factor.getTabelValue(j));
					if ((j % domainSize) == (domainSize - 1)) {
						writer.println();
					} else {
						writer.print(" ");
					}
				}
				writer.println();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != writer) {
				writer.close();
			}
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (4 != args.length) {
			System.out
					.println("java -jar FODParam <input-uai-file> <training-data> <test-data> <output-uai-file>");
			System.exit(-1);
		}

		String input_uai = args[0];
		String training_data = args[1];
		String test_data = args[2];
		String output_uai = args[3];

		
		GraphicalModel model = new GraphicalModel(input_uai, false, false);
		GraphicalModel origModel = new GraphicalModel(input_uai, true);
		//model.initTabelWithoutSettingValue();
		model.initEmptyFactor();
		
		//model.initTabelWithoutSettingValue();
		ChowLiu chowLiu = new ChowLiu();

		chowLiu.model = model;
		chowLiu.origModel = origModel;
		chowLiu.initMutualInfoPair();
		chowLiu.readTrainingDataOnFile(training_data);
		chowLiu.computeMutualInfo();
		//double logLikelihoodDiff = expectMax.testLikelihoodOnFileAndCompare(test_data);

		// FileOutputStream output = new FileOutputStream(output_uai);
		System.out.println("______________________________________________________");
		//System.out.println("log likelihood difference = " + logLikelihoodDiff);
		System.out.println("______________________________________________________");

		//expectMax.dumpNetworkAsUAI(output_uai);
	}

}
