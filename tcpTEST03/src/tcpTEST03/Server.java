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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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

public class Server {

	private JFrame frame;
	private JTextArea contentArea;
	private JTextField txt_message;
	private JTextField txt_max;
	private JTextField txt_port;
	private JButton btn_start;
	private JButton btn_stop;
	private JButton btn_send;
	
//	private JButton btn_file;
	
	private JPanel northPanel;
	private JPanel southPanel;
	private JScrollPane rightPanel;
	private JScrollPane leftPanel;
	private JSplitPane centerSplit;
	private JList userList;
	private DefaultListModel listModel;

	private ServerSocket serverSocket;
	private ServerThread serverThread;
	private ArrayList<ClientThread> clients;

	private boolean isStart = false;
	
	private static int private_key = 2;
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
		public_key_by_client = G[3 - 1];
		Super_key = G[private_key*3 - 1];
	}

	// �D��k,�{������J�f
	public static void main(String[] args) {
		init();
		new Server();
	}

	// ����T���ǰe
	public void send() {
		if (!isStart) {
			JOptionPane.showMessageDialog(frame, "���A���٥��Ұ�,����ǰe�T��!", "���~",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (clients.size() == 0) {
			JOptionPane.showMessageDialog(frame, "�S���ϥΪ̽u�W,����ǰe�T��!", "���~",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = txt_message.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame, "�T�����ର��!", "���~",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		int len = message.length();
		String decrypt_text = "";
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
			decrypt_text += c;
		}
		System.out.println("test");
		sendServerMessage(decrypt_text);// �s�o���A���T��
		contentArea.append("���A����:" + txt_message.getText() + "\r\n");
		txt_message.setText(null);
	}

	// �c�y��k
	public Server() {
		frame = new JFrame("���A��");
		// ���JFrame���ϥ�:
		//frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
		frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Server.class.getResource("qq.png")));
		contentArea = new JTextArea();
		contentArea.setEditable(false);
		contentArea.setForeground(Color.blue);
		txt_message = new JTextField();
		txt_max = new JTextField("30");
		txt_port = new JTextField("6666");
		btn_start = new JButton("�Ұ�");
		btn_stop = new JButton("����");
		btn_send = new JButton("�ǰe");
		
//		btn_file = new JButton("�ǰe�ɮ�");
		
		btn_stop.setEnabled(false);
		listModel = new DefaultListModel();
		userList = new JList(listModel);

		southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(new TitledBorder("�g�T��"));
		
		southPanel.add(txt_message, "Center");
		
		southPanel.add(btn_send, "East");
		
//		southPanel.add(btn_file, "West");
		
		leftPanel = new JScrollPane(userList);
		leftPanel.setBorder(new TitledBorder("�u�W�ϥΪ�"));

		rightPanel = new JScrollPane(contentArea);
		rightPanel.setBorder(new TitledBorder("�T����ܰ�"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
				rightPanel);
		centerSplit.setDividerLocation(100);
		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 6));
		northPanel.add(new JLabel("�H�ƤW��"));
		northPanel.add(txt_max);
		northPanel.add(new JLabel("��"));
		northPanel.add(txt_port);
		northPanel.add(btn_start);
		northPanel.add(btn_stop);
		northPanel.setBorder(new TitledBorder("�t�m��T"));

		frame.setLayout(new BorderLayout());
		frame.add(northPanel, "North");
		frame.add(centerSplit, "Center");
		frame.add(southPanel, "South");
		frame.setSize(600, 400);
		//frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());//�]�w����
		int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		frame.setLocation((screen_width - frame.getWidth()) / 2,
				(screen_height - frame.getHeight()) / 2);
		frame.setVisible(true);

		// ���������ɨƥ�
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isStart) {
					closeServer();// �������A��
				}
				System.exit(0);// �h�X�{��
			}
		});

		// ��r�ث�Enter��ɨƥ�
		txt_message.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		// �����o�e���s�ɨƥ�
		btn_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				send();
			}
		});
		
//		btn_file.addActionListener(new ActionListener()  {			//����ɮ�
//			public void actionPerformed(ActionEvent arg0) {
//				JFileChooser fileChooser = new JFileChooser("C:\\Users");
//				int returnValue = fileChooser.showOpenDialog(null);
//				if (returnValue == JFileChooser.APPROVE_OPTION) //�P�_�O�_����ɮ� 
//				{ 
//					File selectedFile = fileChooser.getSelectedFile();//������File 
//					System.out.println(selectedFile.getName()); //�L�X�ɦW 
//				} 
//			}
//		});

		// �����Ұʦ��A�����s�ɨƥ�
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isStart) {
					JOptionPane.showMessageDialog(frame, "���A���w�B��Ұʪ��A,���n���ƱҰ�!",
							"���~", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int max;
				int port;
				try {
					try {
						max = Integer.parseInt(txt_max.getText());
					} catch (Exception e1) {
						throw new Exception("�H�ƤW���������!");
					}
					if (max <= 0) {
						throw new Exception("�H�ƤW���������!");
					}
					try {
						port = Integer.parseInt(txt_port.getText());
					} catch (Exception e1) {
						throw new Exception("�𸹬������!");
					}
					if (port <= 0) {
						throw new Exception("�� �������!");
					}
					serverStart(max, port);
					contentArea.append("���A���w���\�Ұ�!�H�ƤW��:" + max + ",��:" + port
							+ "\r\n");
					JOptionPane.showMessageDialog(frame, "���A�����\�Ұ�!");
					btn_start.setEnabled(false);
					txt_max.setEnabled(false);
					txt_port.setEnabled(false);
					btn_stop.setEnabled(true);
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"���~", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// ����������A�����s�ɨƥ�
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isStart) {
					JOptionPane.showMessageDialog(frame, "���A���٥��Ұ�,�L�ݰ���!", "���~",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					closeServer();
					btn_start.setEnabled(true);
					txt_max.setEnabled(true);
					txt_port.setEnabled(true);
					btn_stop.setEnabled(false);
					contentArea.append("���A�����\����!\r\n");
					JOptionPane.showMessageDialog(frame, "���A�����\����!");
					serverThread.stop();
					serverSocket.close();
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, "������A���o�Ͳ��`!", "���~",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	// �Ұʦ��A��
	public void serverStart(int max, int port) throws java.net.BindException {
		try {
			clients = new ArrayList<ClientThread>();
			serverSocket = new ServerSocket(port);
			serverThread = new ServerThread(serverSocket, max);
			serverThread.start();
			isStart = true;
		} catch (BindException e) {
			isStart = false;
			throw new BindException("�𸹤w�Q����,�д��@��!");
		} catch (Exception e1) {
			e1.printStackTrace();
			isStart = false;
			throw new BindException("�Ұʦ��A�����`!");
		}
	}

	// �������A��
	@SuppressWarnings("deprecation")
	public void closeServer() {
		try {
			if (serverThread != null)
				serverThread.stop();// ������A�������

			for (int i = clients.size() - 1; i >= 0; i--) {
				// ���Ҧ��u�W�ϥΪ̶ǰe�����R�O
				clients.get(i).getWriter().println("CLOSE");
				clients.get(i).getWriter().flush();
				// ����귽
				clients.get(i).stop();// ��������Ȥ�ݪA�Ȫ������
				clients.get(i).reader.close();
				clients.get(i).writer.close();
				clients.get(i).socket.close();
				clients.remove(i);
			}
			if (serverSocket != null) {
				serverSocket.close();// �������A���ݳs�u
			}
			listModel.removeAllElements();// �M�ŨϥΪ̦C��
			isStart = false;
		} catch (IOException e) {
			e.printStackTrace();
			isStart = true;
		}
	}

	// �s�o���A���T��
	public void sendServerMessage(String message) {
		for (int i = clients.size() - 1; i >= 0; i--) {
			clients.get(i).getWriter().println("���A��:" + message);
			clients.get(i).getWriter().flush();
		}
	}

	// ���A�������
	class ServerThread extends Thread {
		private ServerSocket serverSocket;
		private int max;// �H�ƤW��

		// ���A����������c�y��k
		public ServerThread(ServerSocket serverSocket, int max) {
			this.serverSocket = serverSocket;
			this.max = max;
		}

		public void run() {
			while (true) {// ���������ݫȤ�ݪ��s��
				try {
					Socket socket = serverSocket.accept();
					if (clients.size() == max) {// �p�G�w�F�H�ƤW��
						BufferedReader r = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));
						PrintWriter w = new PrintWriter(socket
								.getOutputStream());
						// �����Ȥ�ݪ��򥻨ϥΪ̸�T
						String inf = r.readLine();
						StringTokenizer st = new StringTokenizer(inf, "@");
						User user = new User(st.nextToken(), st.nextToken());
						// ���X�s�u���\��T
						w.println("MAX@���A��:�藍�_," + user.getName()
								+ user.getIp() + ",���A���u�W�H�Ƥw�F�W��,�еy����ճs�u!");
						w.flush();
						// ����귽
						r.close();
						w.close();
						socket.close();
						continue;
					}
					ClientThread client = new ClientThread(socket);
					client.start();// �}�ҹ惡�Ȥ�ݪA�Ȫ������
					clients.add(client);
					listModel.addElement(client.getUser().getName());// ��s�u�W�C��
					contentArea.append(client.getUser().getName()
							+ client.getUser().getIp() + "�W�u!\r\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// ���@�ӫȤ�ݪA�Ȫ������
	class ClientThread extends Thread {
		private Socket socket;
		private BufferedReader reader;
		private PrintWriter writer;
		private User user;

		public BufferedReader getReader() {
			return reader;
		}

		public PrintWriter getWriter() {
			return writer;
		}

		public User getUser() {
			return user;
		}

		// �Ȥ�ݰ�������c�y��k
		public ClientThread(Socket socket) {
			try {
				this.socket = socket;
				reader = new BufferedReader(new InputStreamReader(socket
						.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				// �����Ȥ�ݪ��򥻨ϥΪ̸�T
				String inf = reader.readLine();
				StringTokenizer st = new StringTokenizer(inf, "@");
				user = new User(st.nextToken(), st.nextToken());
				// ���X�s�u���\��T
				writer.println(user.getName() + user.getIp() + "�P���A���s�u���\!");
				writer.flush();
				// ���X��e�u�W�ϥΪ̸�T
				if (clients.size() > 0) {
					String temp = "";
					for (int i = clients.size() - 1; i >= 0; i--) {
						temp += (clients.get(i).getUser().getName() + "/" + clients
								.get(i).getUser().getIp())
								+ "@";
					}
					writer.println("USERLIST@" + clients.size() + "@" + temp);
					writer.flush();
				}
				// �V�Ҧ��u�W�ϥΪ̶ǰe�ӨϥΪ̤W�u�R�O
				for (int i = clients.size() - 1; i >= 0; i--) {
					clients.get(i).getWriter().println(
							"ADD@" + user.getName() + user.getIp());
					clients.get(i).getWriter().flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("deprecation")
		public void run() {// ���_�����Ȥ�ݪ��T��,�i��B�z�C
			String message = null;
			while (true) {
				try {
					message = reader.readLine();// �����Ȥ�ݰT��
					if (message.equals("CLOSE"))// �U�u�R�O
					{
						contentArea.append(this.getUser().getName()
								+ this.getUser().getIp() + "�U�u!\r\n");
						// �_�}�s�u����귽
						reader.close();
						writer.close();
						socket.close();

						// �V�Ҧ��u�W�ϥΪ̶ǰe�ӨϥΪ̪��U�u�R�O
						for (int i = clients.size() - 1; i >= 0; i--) {
							clients.get(i).getWriter().println(
									"DELETE@" + user.getName());
							clients.get(i).getWriter().flush();
						}

						listModel.removeElement(user.getName());// ��s�u�W�C��

						// �R�������Ȥ�ݪA�Ȱ����
						for (int i = clients.size() - 1; i >= 0; i--) {
							if (clients.get(i).getUser() == user) {
								ClientThread temp = clients.get(i);
								clients.remove(i);// �R�����ϥΪ̪��A�Ȱ����
								temp.stop();// ����o���A�Ȱ����
								return;
							}
						}
					} else {
						dispatcherMessage(message);// ��o�T��
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// ��o�T��
		public void dispatcherMessage(String message) {
			StringTokenizer stringTokenizer = new StringTokenizer(message, "@");
			String source = stringTokenizer.nextToken();
			String owner = stringTokenizer.nextToken();
			String content = stringTokenizer.nextToken();
			int len = content.length();
			String cipher = "";
			for (int i = 0; i < len; i++) {
				char c = content.charAt(i);
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
				cipher += c;
			}
			cipher = source +  "��:" + cipher;
			message = source + "��:" + content;
			contentArea.append(cipher + "\r\n");
			if (owner.equals("ALL")) {// �s�o
				for (int i = clients.size() - 1; i >= 0; i--) {
					clients.get(i).getWriter().println(message);
					clients.get(i).getWriter().flush();
				}
			}
		}
	}
}
