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

	// 主方法,程式執行入口
	public static void main(String[] args) {
		init();
		new Server();
	}

	// 執行訊息傳送
	public void send() {
		if (!isStart) {
			JOptionPane.showMessageDialog(frame, "伺服器還未啟動,不能傳送訊息!", "錯誤",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (clients.size() == 0) {
			JOptionPane.showMessageDialog(frame, "沒有使用者線上,不能傳送訊息!", "錯誤",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = txt_message.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame, "訊息不能為空!", "錯誤",
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
		sendServerMessage(decrypt_text);// 群發伺服器訊息
		contentArea.append("伺服器說:" + txt_message.getText() + "\r\n");
		txt_message.setText(null);
	}

	// 構造放法
	public Server() {
		frame = new JFrame("伺服器");
		// 更改JFrame的圖示:
		//frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Client.class.getResource("qq.png")));
		frame.setIconImage(Toolkit.getDefaultToolkit().createImage(Server.class.getResource("qq.png")));
		contentArea = new JTextArea();
		contentArea.setEditable(false);
		contentArea.setForeground(Color.blue);
		txt_message = new JTextField();
		txt_max = new JTextField("30");
		txt_port = new JTextField("6666");
		btn_start = new JButton("啟動");
		btn_stop = new JButton("停止");
		btn_send = new JButton("傳送");
		
//		btn_file = new JButton("傳送檔案");
		
		btn_stop.setEnabled(false);
		listModel = new DefaultListModel();
		userList = new JList(listModel);

		southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(new TitledBorder("寫訊息"));
		
		southPanel.add(txt_message, "Center");
		
		southPanel.add(btn_send, "East");
		
//		southPanel.add(btn_file, "West");
		
		leftPanel = new JScrollPane(userList);
		leftPanel.setBorder(new TitledBorder("線上使用者"));

		rightPanel = new JScrollPane(contentArea);
		rightPanel.setBorder(new TitledBorder("訊息顯示區"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel,
				rightPanel);
		centerSplit.setDividerLocation(100);
		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 6));
		northPanel.add(new JLabel("人數上限"));
		northPanel.add(txt_max);
		northPanel.add(new JLabel("埠"));
		northPanel.add(txt_port);
		northPanel.add(btn_start);
		northPanel.add(btn_stop);
		northPanel.setBorder(new TitledBorder("配置資訊"));

		frame.setLayout(new BorderLayout());
		frame.add(northPanel, "North");
		frame.add(centerSplit, "Center");
		frame.add(southPanel, "South");
		frame.setSize(600, 400);
		//frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());//設定全屏
		int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
		int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;
		frame.setLocation((screen_width - frame.getWidth()) / 2,
				(screen_height - frame.getHeight()) / 2);
		frame.setVisible(true);

		// 關閉視窗時事件
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isStart) {
					closeServer();// 關閉伺服器
				}
				System.exit(0);// 退出程式
			}
		});

		// 文字框按Enter鍵時事件
		txt_message.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		// 單擊發送按鈕時事件
		btn_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				send();
			}
		});
		
//		btn_file.addActionListener(new ActionListener()  {			//選擇檔案
//			public void actionPerformed(ActionEvent arg0) {
//				JFileChooser fileChooser = new JFileChooser("C:\\Users");
//				int returnValue = fileChooser.showOpenDialog(null);
//				if (returnValue == JFileChooser.APPROVE_OPTION) //判斷是否選擇檔案 
//				{ 
//					File selectedFile = fileChooser.getSelectedFile();//指派給File 
//					System.out.println(selectedFile.getName()); //印出檔名 
//				} 
//			}
//		});

		// 單擊啟動伺服器按鈕時事件
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (isStart) {
					JOptionPane.showMessageDialog(frame, "伺服器已處於啟動狀態,不要重複啟動!",
							"錯誤", JOptionPane.ERROR_MESSAGE);
					return;
				}
				int max;
				int port;
				try {
					try {
						max = Integer.parseInt(txt_max.getText());
					} catch (Exception e1) {
						throw new Exception("人數上限為正整數!");
					}
					if (max <= 0) {
						throw new Exception("人數上限為正整數!");
					}
					try {
						port = Integer.parseInt(txt_port.getText());
					} catch (Exception e1) {
						throw new Exception("埠號為正整數!");
					}
					if (port <= 0) {
						throw new Exception("埠號 為正整數!");
					}
					serverStart(max, port);
					contentArea.append("伺服器已成功啟動!人數上限:" + max + ",埠:" + port
							+ "\r\n");
					JOptionPane.showMessageDialog(frame, "伺服器成功啟動!");
					btn_start.setEnabled(false);
					txt_max.setEnabled(false);
					txt_port.setEnabled(false);
					btn_stop.setEnabled(true);
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"錯誤", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// 單擊停止伺服器按鈕時事件
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isStart) {
					JOptionPane.showMessageDialog(frame, "伺服器還未啟動,無需停止!", "錯誤",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					closeServer();
					btn_start.setEnabled(true);
					txt_max.setEnabled(true);
					txt_port.setEnabled(true);
					btn_stop.setEnabled(false);
					contentArea.append("伺服器成功停止!\r\n");
					JOptionPane.showMessageDialog(frame, "伺服器成功停止!");
					serverThread.stop();
					serverSocket.close();
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, "停止伺服器發生異常!", "錯誤",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
	}

	// 啟動伺服器
	public void serverStart(int max, int port) throws java.net.BindException {
		try {
			clients = new ArrayList<ClientThread>();
			serverSocket = new ServerSocket(port);
			serverThread = new ServerThread(serverSocket, max);
			serverThread.start();
			isStart = true;
		} catch (BindException e) {
			isStart = false;
			throw new BindException("埠號已被佔用,請換一個!");
		} catch (Exception e1) {
			e1.printStackTrace();
			isStart = false;
			throw new BindException("啟動伺服器異常!");
		}
	}

	// 關閉伺服器
	@SuppressWarnings("deprecation")
	public void closeServer() {
		try {
			if (serverThread != null)
				serverThread.stop();// 停止伺服器執行緒

			for (int i = clients.size() - 1; i >= 0; i--) {
				// 給所有線上使用者傳送關閉命令
				clients.get(i).getWriter().println("CLOSE");
				clients.get(i).getWriter().flush();
				// 釋放資源
				clients.get(i).stop();// 停止此條為客戶端服務的執行緒
				clients.get(i).reader.close();
				clients.get(i).writer.close();
				clients.get(i).socket.close();
				clients.remove(i);
			}
			if (serverSocket != null) {
				serverSocket.close();// 關閉伺服器端連線
			}
			listModel.removeAllElements();// 清空使用者列表
			isStart = false;
		} catch (IOException e) {
			e.printStackTrace();
			isStart = true;
		}
	}

	// 群發伺服器訊息
	public void sendServerMessage(String message) {
		for (int i = clients.size() - 1; i >= 0; i--) {
			clients.get(i).getWriter().println("伺服器:" + message);
			clients.get(i).getWriter().flush();
		}
	}

	// 伺服器執行緒
	class ServerThread extends Thread {
		private ServerSocket serverSocket;
		private int max;// 人數上限

		// 伺服器執行緒的構造方法
		public ServerThread(ServerSocket serverSocket, int max) {
			this.serverSocket = serverSocket;
			this.max = max;
		}

		public void run() {
			while (true) {// 不停的等待客戶端的連結
				try {
					Socket socket = serverSocket.accept();
					if (clients.size() == max) {// 如果已達人數上限
						BufferedReader r = new BufferedReader(
								new InputStreamReader(socket.getInputStream()));
						PrintWriter w = new PrintWriter(socket
								.getOutputStream());
						// 接收客戶端的基本使用者資訊
						String inf = r.readLine();
						StringTokenizer st = new StringTokenizer(inf, "@");
						User user = new User(st.nextToken(), st.nextToken());
						// 反饋連線成功資訊
						w.println("MAX@伺服器:對不起," + user.getName()
								+ user.getIp() + ",伺服器線上人數已達上限,請稍後嘗試連線!");
						w.flush();
						// 釋放資源
						r.close();
						w.close();
						socket.close();
						continue;
					}
					ClientThread client = new ClientThread(socket);
					client.start();// 開啟對此客戶端服務的執行緒
					clients.add(client);
					listModel.addElement(client.getUser().getName());// 更新線上列表
					contentArea.append(client.getUser().getName()
							+ client.getUser().getIp() + "上線!\r\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// 為一個客戶端服務的執行緒
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

		// 客戶端執行緒的構造方法
		public ClientThread(Socket socket) {
			try {
				this.socket = socket;
				reader = new BufferedReader(new InputStreamReader(socket
						.getInputStream()));
				writer = new PrintWriter(socket.getOutputStream());
				// 接收客戶端的基本使用者資訊
				String inf = reader.readLine();
				StringTokenizer st = new StringTokenizer(inf, "@");
				user = new User(st.nextToken(), st.nextToken());
				// 反饋連線成功資訊
				writer.println(user.getName() + user.getIp() + "與伺服器連線成功!");
				writer.flush();
				// 反饋當前線上使用者資訊
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
				// 向所有線上使用者傳送該使用者上線命令
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
		public void run() {// 不斷接收客戶端的訊息,進行處理。
			String message = null;
			while (true) {
				try {
					message = reader.readLine();// 接收客戶端訊息
					if (message.equals("CLOSE"))// 下線命令
					{
						contentArea.append(this.getUser().getName()
								+ this.getUser().getIp() + "下線!\r\n");
						// 斷開連線釋放資源
						reader.close();
						writer.close();
						socket.close();

						// 向所有線上使用者傳送該使用者的下線命令
						for (int i = clients.size() - 1; i >= 0; i--) {
							clients.get(i).getWriter().println(
									"DELETE@" + user.getName());
							clients.get(i).getWriter().flush();
						}

						listModel.removeElement(user.getName());// 更新線上列表

						// 刪除此條客戶端服務執行緒
						for (int i = clients.size() - 1; i >= 0; i--) {
							if (clients.get(i).getUser() == user) {
								ClientThread temp = clients.get(i);
								clients.remove(i);// 刪除此使用者的服務執行緒
								temp.stop();// 停止這條服務執行緒
								return;
							}
						}
					} else {
						dispatcherMessage(message);// 轉發訊息
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// 轉發訊息
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
			cipher = source +  "說:" + cipher;
			message = source + "說:" + content;
			contentArea.append(cipher + "\r\n");
			if (owner.equals("ALL")) {// 群發
				for (int i = clients.size() - 1; i >= 0; i--) {
					clients.get(i).getWriter().println(message);
					clients.get(i).getWriter().flush();
				}
			}
		}
	}
}
