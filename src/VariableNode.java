
public class VariableNode implements Comparable<VariableNode> {
		Variable variable;
		double key = Double.NEGATIVE_INFINITY;
		VariableNode prev = null;
		
		public VariableNode(Variable var) {
			variable = var;
		}

		@Override
		public int compareTo(VariableNode o) {
			// TODO Auto-generated method stub
			return Double.compare(o.key, this.key);
		}
	}