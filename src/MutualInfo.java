import java.util.ArrayList;



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