import java.util.ArrayList;


public class MaxHeap {
	private ArrayList<VariableNode> vertices;
	private int[] Q;
	private int[] X;
	private int heapSize = 0;

	public MaxHeap(ArrayList<VariableNode> verticesRef) {
		int size = verticesRef.size();
		vertices = verticesRef;
		Q = new int[size + 1];
		X = new int[size];
	}

	public MaxHeap() {
		// TODO Auto-generated constructor stub
	}

	public void buildHeap() {
		heapSize = vertices.size();

		for (int i = 0; i < vertices.size(); i++) {
			Q[i + 1] = i;
			X[i] = i + 1;
		}

		for (int i = heapSize / 2; i > 0; i--) {
			percolateDown(i);
		}
	}

	private void exchange(int i, int j) {
		int tmp = Q[i];
		Q[i] = Q[j];
		Q[j] = tmp;
		X[Q[i]] = i;
		X[Q[j]] = j;
	}

	private void percolateDown(int hole) {
		int child;

		for (; hole * 2 <= heapSize; hole = child) {
			child = hole * 2;
			if (child != heapSize
					&& vertices.get(Q[child + 1]).compareTo(vertices.get(Q[child])) < 0)
				child++;
			if (vertices.get(Q[child]).compareTo(vertices.get(Q[hole])) < 0)
				exchange(hole, child);
			else
				break;
		}
	}

	public int deleteMax() {
		int maxItem = Q[1];
		Q[1] = Q[heapSize--];
		percolateDown(1);

		return maxItem;
	}

	// public int insert(Vertex newVertex) {
	// int hole = ++heapSize;
	// array[hole] = newVertex;
	// hole = percolateUp(hole);
	// return hole;
	// }
	//
	private int percolateUp(int hole) {
		for (Q[0] = Q[hole]; vertices.get(Q[hole]).compareTo(vertices.get(Q[hole / 2])) < 0; hole /= 2) {
			exchange(hole, hole / 2);
		}
		return hole;
	}

	public boolean isEmpty() {
		return (heapSize == 0) ? true : false;
	}

	public void increaseElement(int index, double newDis) {
		int a = X[index];

		if (vertices.get(index).key > newDis)
			return;

		vertices.get(index).key = newDis;

		percolateUp(a);
	}
}
