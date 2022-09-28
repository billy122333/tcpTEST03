package tcpTEST03;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

public class Client{

	private JFrame frame;
	private JList userList;
	private JTextArea textArea;
	private JTextField textField;
	private JTextField txt_port;
	private JTextField txt_hostIp;
	private JTextField txt_name;
	private JButton btn_start;
	private JButton btn_stop;
	private JButton btn_send;
	private JPanel northPanel;
	private JPanel southPanel;
	private JScrollPane rightScroll;
	private JScrollPane leftScroll;
	private JSplitPane centerSplit;

	private DefaultListModel listModel;
	private boolean isConnected = false;

	private Socket socket;
	private PrintWriter writer;
	private BufferedReader reader;
	private MessageThread messageThread;// �t�d�����T���������
	private Map<String, User> onLineUsers = new HashMap<String, User>();// �Ҧ��u�W�ϥΪ�

	private static int private_key = 3;
	private static ECC ecc = new ECC(2, 2, 17);
	private static Node[]G = new Node[18];
	public static Node public_key = G[private_key - 1];
	private static Node public_key_by_client;
	private static Node Super_key;
	public static void init() {
		G[0] = new Node(5, 1);
		for(int i = 1; i < 18; i++) {
			G[i] = new Node().add(G[i - 1], G[0], ecc);
		}
		public_key_by_client = G[2 - 1];
		Super_key = G[private_key*2 - 1];
	}
	
	// �D��k,�{���J�f
	public static void main(String[] args) {
		init();
		new Client();
	}

	// ����ǰe
	public void send() {
		if (!isConnected) {
			JOptionPane.showMessageDialog(frame, "�٨S���s�u���A��,�L�k�ǰe�T��!", "���~",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = textField.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame, "�T�����ର��!", "���~",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		int len = message.length();
		String cipher = "";
		for (int i = 0; i < len; i++) {
			char c = message.charAt(i);
			int j;
			if (c >= 'a' && c <= 'z') {
				j = c - 'a' + Super_key.get_x();
				j %= 26;
				c = (char)('a' + j);
			}
			else if (c >= 'A' && c <= 'Z') {
				j = c - 'A' + Super_key.get_x();
				j %= 26;
				c = (char)('A' + j);
			}
			cipher += c;
		}
		System.out.println("test");
		sendMessage(frame.getTitle() + "@" + "ALL" + "@" + cipher);
		textField.setText(null);
	}

	// �c�y��k
	public Client() {
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setForeground(Color.blue);
		textField = new JTextField();
		txt_port = new JTextField("6666");
		txt_hostIp = new JTextField("127.0.0.1");
		txt_name = new JTextField("xiaoqiang");
		btn_start = new JButton("�s�u");
		btn_stop = new JButton("�_�}");
		btn_send = new JButton("�ǰe");
		listModel = new DefaultListModel();
		userList = new JList(listModel);

		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 7));
		northPanel.add(new JLabel("��"));
		northPanel.add(txt_port);
		northPanel.add(new JLabel("���A��IP"));
		northPanel.add(txt_hostIp);
		northPanel.add(new JLabel("�m�W"));
		northPanel.add(txt_name);
		northPanel.add(btn_start);
		northPanel.add(btn_stop);
		northPanel.setBorder(new TitledBorder("�s�u��T"));

		rightScroll = new JScrollPane(textArea);
		rightScroll.setBorder(new TitledBorder("�T����ܰ�"));
		
		leftScroll = new JScrollPane(userList);
		leftScroll.setBorder(new TitledBorder("�u�W�ϥΪ�"));
		
		southPanel = new JPanel(new BorderLayout());
		southPanel.add(textField, "Center");
		southPanel.add(btn_send, "East");
		southPanel.setBorder(new TitledBorder("�g�T��"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
				rightScroll);
		centerSplit.setDividerLocation(100);

		frame = new JFrame("�Ȥ��");
		// ���JFrame���ϥ�:
		frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
		frame.setLayout(new BorderLayout());
		frame.add(northPanel, "North");
		frame.add(centerSplit, "Center");
		frame.add(southPanel, "South");
		frame.setSize(600, 400);
		int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		frame.setLocation((screen_width - frame.getWidth()) / 2,
				(screen_height - frame.getHeight()) / 2);
		frame.setVisible(true);

		// �g�T������r�ؤ����^����ɨƥ�
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				send();
			}
		});

		// �����o�e���s�ɨƥ�
		btn_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		// �����s�u���s�ɨƥ�
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int port;
				if (isConnected) {
					JOptionPane.showMessageDialog(frame, "�w�B��s�u�W���A,���n���Ƴs�u!",
							"���~", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					try {
						port = Integer.parseInt(txt_port.getText().trim());
					} catch (NumberFormatException e2) {
						throw new Exception("�𸹤��ŦX�n�D!�𬰾��!");
					}
					String hostIp = txt_hostIp.getText().trim();
					String name = txt_name.getText().trim();
					if (name.equals("") || hostIp.equals("")) {
						throw new Exception("�m�W�B���A��IP���ର��!");
					}
					boolean flag = connectServer(port, hostIp, name);
					if (flag == false) {
						throw new Exception("�P���A���s�u����!");
					}
					frame.setTitle(name);
					JOptionPane.showMessageDialog(frame, "���\�s�u!");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"���~", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// �����_�}���s�ɨƥ�
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isConnected) {
					JOptionPane.showMessageDialog(frame, "�w�B���_�}���A,���n�����_�}!",
							"���~", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					boolean flag = closeConnection();// �_�}�s�u
					if (flag == false) {
						throw new Exception("�_�}�s�u�o�Ͳ��`!");
					}
					JOptionPane.showMessageDialog(frame, "���\�_�}!");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"���~", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// ���������ɨƥ�
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isConnected) {
					closeConnection();// �����s�u
				}
				System.exit(0);// �h�X�{��
			}
		});
	}

	/**
	 * �s�u���A��
	 * 
	 * @param port
	 * @param hostIp
	 * @param name
	 */
	public boolean connectServer(int port, String hostIp, String name) {
		// �s�u���A��
		try {
			socket = new Socket(hostIp, port);// �ھڰ𸹩M���A��ip�إ߳s�u
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			// �ǰe�Ȥ�ݨϥΪ̰򥻸�T(�ϥΪ̦W�٩Mip�a�})
			sendMessage(name + "@" + socket.getLocalAddress().toString());
			// �}�ұ����T���������
			messageThread = new MessageThread(reader, textArea);
			messageThread.start();
			isConnected = true;// �w�g�s�u�W�F
			return true;
		} catch (Exception e) {
			textArea.append("�P�𸹬�:" + port + "    IP�a�}��:" + hostIp
					+ "   �����A���s�u����!" + "\r\n");
			isConnected = false;// ���s�u�W
			return false;
		}
	}

	/**
	 * �ǰe�T��
	 * 
	 * @param message
	 */
	public void sendMessage(String message) {
		writer.println(message);
		writer.flush();
	}

	/**
	 * �Ȥ�ݥD�������s�u
	 */
	@SuppressWarnings("deprecation")
	public synchronized boolean closeConnection() {
		try {
			sendMessage("CLOSE");// �ǰe�_�}�s�u�R�O�����A��
			messageThread.stop();// ������T�������
			// ����귽
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (socket != null) {
				socket.close();
			}
			isConnected = false;
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			isConnected = true;
			return false;
		}
	}

	// ���_�����T���������
	class MessageThread extends Thread {
		private BufferedReader reader;
		private JTextArea textArea;

		// �����T����������c�y��k
		public MessageThread(BufferedReader reader, JTextArea textArea) {
			this.reader = reader;
			this.textArea = textArea;
		}

		// �Q�ʪ������s�u
		public synchronized void closeCon() throws Exception {
			// �M�ŨϥΪ̦C��
			listModel.removeAllElements();
			// �Q�ʪ������s�u����귽
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (socket != null) {
				socket.close();
			}
			isConnected = false;// �ק窱�A���_�}
		}

		public void run() {
			String message = "";
			while (true) {
				try {
					message = reader.readLine();
					StringTokenizer stringTokenizer = new StringTokenizer(
							message, "/@");
					String command = stringTokenizer.nextToken();// �R�O
					if (command.equals("CLOSE"))// ���A���w�����R�O
					{
						textArea.append("���A���w����!\r\n");
						closeCon();// �Q�ʪ������s�u
						return;// ���������
					} else if (command.equals("ADD")) {// ���ϥΪ̤W�u��s�u�W�C��
						String username = "";
						String userIp = "";
						if ((username = stringTokenizer.nextToken()) != null
								&& (userIp = stringTokenizer.nextToken()) != null) {
							User user = new User(username, userIp);
							onLineUsers.put(username, user);
							listModel.addElement(username);
						}
					} else if (command.equals("DELETE")) {// ���ϥΪ̤U�u��s�u�W�C��
						String username = stringTokenizer.nextToken();
						User user = (User) onLineUsers.get(username);
						onLineUsers.remove(user);
						listModel.removeElement(username);
					} else if (command.equals("USERLIST")) {// ���J�u�W�ϥΪ̦C��
						int size = Integer
								.parseInt(stringTokenizer.nextToken());
						String username = null;
						String userIp = null;
						for (int i = 0; i < size; i++) {
							username = stringTokenizer.nextToken();
							userIp = stringTokenizer.nextToken();
							User user = new User(username, userIp);
							onLineUsers.put(username, user);
							listModel.addElement(username);
						}
					} else if (command.equals("MAX")) {// �H�Ƥw�F�W��
						textArea.append(stringTokenizer.nextToken()
								+ stringTokenizer.nextToken() + "\r\n");
						closeCon();// �Q�ʪ������s�u
						JOptionPane.showMessageDialog(frame, "���A���w�İϤw��!", "���~",
								JOptionPane.ERROR_MESSAGE);
						return;// ���������
					} else {// ���q�T��
						String[] strs = message.split(":");
						if (strs.length != 1) {
							int len = strs[1].length();
							String decrypt_text = "";
							for (int i = 0; i < len; i++) {
								char c = strs[1].charAt(i);
								int j;
								if (c >= 'a' && c <= 'z') {
									j = c - 'a' + (26 - Super_key.get_x());
									j %= 26;
									c = (char)('a' + j);
								}
								else if (c >= 'A' && c <= 'Z') {
									j = c - 'A' + (26 - Super_key.get_x());
									j %= 26;
									c = (char)('A' + j);
								}
								decrypt_text += c;
							}
							System.out.println("test");
							textArea.append(strs[0] + ":" + decrypt_text + "\r\n");
						} else {
							textArea.append(message + "\r\n");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
