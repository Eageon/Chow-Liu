import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class ChowLiu {

	GraphicalModel model;
	GraphicalModel origModel;

	ArrayList<MutualInfo> pairs;
	LinkedList<Evidence> evidenceSet;
	ArrayList<ArrayList<Double>> prVariable;
	ArrayList<ArrayList<Double>> pairVariable;

	MaxHeap maxHeap;

	int numEvidence;

	private void computeVariableProbability() {
		prVariable = new ArrayList<>(model.variables.size());

		for (int i = 0; i < model.variables.size(); i++) {
			Variable var = model.getVariable(i);
			ArrayList<Double> varRespect = new ArrayList<>(var.domainSize());
			int numXi = 0;

			for (int j = 0; j < var.domainSize(); j++) {
				for (Evidence e : evidenceSet) {
					if (e.isConsistentWith(var, j)) {
						numXi++;
					}
				}

				varRespect.add((double) numXi / evidenceSet.size());
			}

			prVariable.add(varRespect);
		}
	}

	private void computePairProbability() {
		pairVariable = new ArrayList<>(pairs.size());

		for (int i = 0; i < pairs.size(); i++) {
			MutualInfo mutualInfo = pairs.get(i);
			ArrayList<Double> pairRespect = new ArrayList<>(
					mutualInfo.tableSize());
			int numXiXj = 0;

			for (int j = 0; j < mutualInfo.tableSize(); j++) {
				int[] values = mutualInfo.tableIndexToVaraibleValue(j);
				mutualInfo.setValues(values);

				for (Evidence e : evidenceSet) {
					if (e.isConsistentWith(mutualInfo.varPair, values)) {
						numXiXj++;
					}
				}
				pairRespect.add((double) numXiXj / evidenceSet.size());
			}
			pairVariable.add(pairRespect);
		}
	}

	public ArrayList<MutualInfo> initMutualInfoPair() {
		int n = model.variables.size();
		pairs = new ArrayList<>((n * n - n) / 2);

		for (int i = 0; i < model.variables.size(); i++) {
			for (int j = i + 1; j < model.variables.size(); j++) {
				MutualInfo mutualInfo = new MutualInfo(model.getVariable(i),
						model.getVariable(j));
				pairs.add(mutualInfo);
			}
		}

		return pairs;
	}

	public ArrayList<MutualInfo> computeMutualInfo() {
		System.out.println("2.1");
		computeVariableProbability();
		System.out.println("2.2");
		computePairProbability();
		System.out.println("2.3");

		for (int i = 0; i < pairs.size(); i++) {
			MutualInfo mutualInfo = pairs.get(i);

			for (int j = 0; j < mutualInfo.tableSize(); j++) {
				int[] values = mutualInfo.tableIndexToVaraibleValue(j);

				mutualInfo.setValues(values);

				double PrD_Xi_and_Xj = pairVariable.get(i).get(j);
				double PrD_Xi = prVariable.get(mutualInfo.from().index).get(
						values[0]);
				double PrD_Xj = prVariable.get(mutualInfo.to().index).get(
						values[1]);

				if (PrD_Xi == 0.0 || PrD_Xj == 0.0) {
					continue;
				}

				mutualInfo.mutualInfo += PrD_Xi_and_Xj
						* (Math.log(PrD_Xi_and_Xj / (PrD_Xi * PrD_Xj)) / Math
								.log(2));
			}
		}

		return pairs;
	}

	public ArrayList<MutualInfo> sortByDescendOrder() {
		// Collections.sort(pairs);
		// Collections.reverse(pairs);

		return pairs;
	}

	public void generateMST() {
		ArrayList<VariableNode> nodeList = new ArrayList<>(
				model.variables.size());

		for (Variable var : model.variables) {
			nodeList.add(new VariableNode(var));
		}

		LinkedList<VariableNode> nodeListCopy = new LinkedList<>(nodeList);
		maxHeap = new MaxHeap(nodeList);

		Variable s = pairs.get(0).from();
		nodeList.get(s.index).key = 0.0;

		while (!maxHeap.isEmpty()) {
			VariableNode u = nodeList.get(maxHeap.deleteMax());
			nodeListCopy.remove(u);

			for (VariableNode v : nodeListCopy) {
				if (weight(u.variable, v.variable) > v.key) {
					v.prev = u;
					maxHeap.increaseElement(v.variable.index,
							weight(u.variable, v.variable));
				}
			}
		}

		for (VariableNode u : nodeList) {
			if (null != u.prev) {
				model.getFactor(u.variable.index).variables.add(0,
						u.prev.variable);
			}
		}
	}

	public double weight(Variable from, Variable to) {
		if (from.index > to.index) {
			Variable tmp = from;
			from = to;
			to = tmp;
		}

		int fromIndex = 0;
		int n = model.variables.size();

		for (int i = 1; i < from.index; i++) {
			fromIndex += (n - i);
		}

		fromIndex += (to.index - from.index - 1);

		return pairs.get(fromIndex).mutualInfo;
	}

	public GraphicalModel generateChowLiuFactor() {
		for (int i = 0; i < pairs.size(); i++) {
			MutualInfo mutualInfo = pairs.get(i);

			if (pathExists(mutualInfo.from(), mutualInfo.to())) {
				continue;
			}

			model.factors.get(mutualInfo.to().index).variables.add(0,
					mutualInfo.from());
		}

		return model;
	}

	public boolean pathExists(Variable from, Variable to) {
		Factor fromFactor = model.getFactor(from.index);
		Factor toFactor = model.getFactor(to.index);

		if (from == to) {
			return true;
		}

		if (toFactor.variables.contains(from)) {
			return true;
		}

		for (int i = 0; i < toFactor.numScopes() - 1; i++) {
			if (pathExists(from, toFactor.variables.get(i))) {
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
		// model.initTabelWithoutSettingValue();
		model.initEmptyFactor();

		ChowLiu chowLiu = new ChowLiu();

		chowLiu.model = model;
		chowLiu.origModel = origModel;
		chowLiu.initMutualInfoPair();
		System.out.println(1);
		chowLiu.readTrainingDataOnFile(training_data);
		System.out.println(2);
		chowLiu.computeMutualInfo();
		System.out.println(3);
		chowLiu.sortByDescendOrder();
		System.out.println(4);
		chowLiu.generateChowLiuFactor();
		System.out.println(5);

		model.initTabelWithoutSettingValue();
		FODParam fodParam = new FODParam(model);

		fodParam.readTrainingDataOnFile(training_data);
		fodParam.runFODParam();
		fodParam.origModel = origModel;
		double logLikelihoodDiff = fodParam
				.testLikelihoodOnFileAndCompare(test_data);
		// double logLikelihoodDiff =
		// expectMax.testLikelihoodOnFileAndCompare(test_data);

		// FileOutputStream output = new FileOutputStream(output_uai);
		System.out
				.println("______________________________________________________");
		System.out.println("log likelihood difference = " + logLikelihoodDiff);
		System.out
				.println("______________________________________________________");

		fodParam.dumpNetworkAsUAI(output_uai);
	}

}
