package tcpTEST03;

public class ECC {
	// $y^2 \equiv x^3 + ax + b \mod mod$
	private int a, b, mod;
	public ECC() {
		a = b = mod = 0;
	}
	public ECC(int ia, int ib, int imod) {
		a = ia;
		b = ib;
		mod = imod;
	}
	public int get_a() {
		return this.a;
	}
	public int get_b() {
		return this.b;
	}
	public int get_mod() {
		return this.mod;
	}
}
