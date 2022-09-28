package tcpTEST03;

public class Node {
	private int x, y;
    public Node() {
        x = y = 0;
    }
    public Node(int i, int j) {
        x = i;
        y = j;
    }
    public int get_x() {
    	return this.x;
    }
    public int get_y() {
    	return this.y;
    }
    public void set_x(int i) {
    	x = i;
    }
    public void set_y(int i) {
    	y = i;
    }
    public Node add(Node p, Node q, ECC ecc) {
    	Node node = new Node();
		int dividend, divisor, m;
		if (p.get_x() == q.get_x() && p.get_y() == q.get_y()) {
			dividend = 3*(p.get_x()*p.get_x()) + ecc.get_a();
			divisor = 2*p.get_y();
		} else {
			dividend = (p.get_y() - q.get_y());
			divisor = (p.get_x() - q.get_x());
		}
		m = extend_modulus(dividend, divisor, ecc.get_mod());
		node.set_x(extend_modulus((m*m - p.get_x() - q.get_x()), 1, ecc.get_mod()));
		node.set_y(extend_modulus(m*(p.get_x() - node.get_x()) - p.get_y(), 1, ecc.get_mod()));
		return node;
	}
    public static int extend_modulus(int dividend, int divisor, int mod) {
    	if (dividend < 0) dividend = dividend%mod + mod;
		if (divisor < 0) divisor = divisor%mod + mod;
		int tmp = divisor;
		while(tmp%mod != 1) {
			tmp += divisor;
		}
		tmp = tmp/divisor;
		int ans = (dividend*tmp)%mod;
		if (ans < 0) ans += mod;
		return ans;
    }
}
