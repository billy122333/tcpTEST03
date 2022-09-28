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
	private MessageThread messageThread;// 負責接收訊息的執行緒
	private Map<String, User> onLineUsers = new HashMap<String, User>();// 所有線上使用者

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
	
	// 主方法,程式入口
	public static void main(String[] args) {
		init();
		new Client();
	}

	// 執行傳送
	public void send() {
		if (!isConnected) {
			JOptionPane.showMessageDialog(frame, "還沒有連線伺服器,無法傳送訊息!", "錯誤",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		String message = textField.getText().trim();
		if (message == null || message.equals("")) {
			JOptionPane.showMessageDialog(frame, "訊息不能為空!", "錯誤",
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

	// 構造方法
	public Client() {
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setForeground(Color.blue);
		textField = new JTextField();
		txt_port = new JTextField("6666");
		txt_hostIp = new JTextField("127.0.0.1");
		txt_name = new JTextField("xiaoqiang");
		btn_start = new JButton("連線");
		btn_stop = new JButton("斷開");
		btn_send = new JButton("傳送");
		listModel = new DefaultListModel();
		userList = new JList(listModel);

		northPanel = new JPanel();
		northPanel.setLayout(new GridLayout(1, 7));
		northPanel.add(new JLabel("埠"));
		northPanel.add(txt_port);
		northPanel.add(new JLabel("伺服器IP"));
		northPanel.add(txt_hostIp);
		northPanel.add(new JLabel("姓名"));
		northPanel.add(txt_name);
		northPanel.add(btn_start);
		northPanel.add(btn_stop);
		northPanel.setBorder(new TitledBorder("連線資訊"));

		rightScroll = new JScrollPane(textArea);
		rightScroll.setBorder(new TitledBorder("訊息顯示區"));
		
		leftScroll = new JScrollPane(userList);
		leftScroll.setBorder(new TitledBorder("線上使用者"));
		
		southPanel = new JPanel(new BorderLayout());
		southPanel.add(textField, "Center");
		southPanel.add(btn_send, "East");
		southPanel.setBorder(new TitledBorder("寫訊息"));

		centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll,
				rightScroll);
		centerSplit.setDividerLocation(100);

		frame = new JFrame("客戶機");
		// 更改JFrame的圖示:
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

		// 寫訊息的文字框中按回車鍵時事件
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				send();
			}
		});

		// 單擊發送按鈕時事件
		btn_send.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});

		// 單擊連線按鈕時事件
		btn_start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int port;
				if (isConnected) {
					JOptionPane.showMessageDialog(frame, "已處於連線上狀態,不要重複連線!",
							"錯誤", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					try {
						port = Integer.parseInt(txt_port.getText().trim());
					} catch (NumberFormatException e2) {
						throw new Exception("埠號不符合要求!埠為整數!");
					}
					String hostIp = txt_hostIp.getText().trim();
					String name = txt_name.getText().trim();
					if (name.equals("") || hostIp.equals("")) {
						throw new Exception("姓名、伺服器IP不能為空!");
					}
					boolean flag = connectServer(port, hostIp, name);
					if (flag == false) {
						throw new Exception("與伺服器連線失敗!");
					}
					frame.setTitle(name);
					JOptionPane.showMessageDialog(frame, "成功連線!");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"錯誤", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// 單擊斷開按鈕時事件
		btn_stop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isConnected) {
					JOptionPane.showMessageDialog(frame, "已處於斷開狀態,不要重複斷開!",
							"錯誤", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try {
					boolean flag = closeConnection();// 斷開連線
					if (flag == false) {
						throw new Exception("斷開連線發生異常!");
					}
					JOptionPane.showMessageDialog(frame, "成功斷開!");
				} catch (Exception exc) {
					JOptionPane.showMessageDialog(frame, exc.getMessage(),
							"錯誤", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// 關閉視窗時事件
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (isConnected) {
					closeConnection();// 關閉連線
				}
				System.exit(0);// 退出程式
			}
		});
	}

	/**
	 * 連線伺服器
	 * 
	 * @param port
	 * @param hostIp
	 * @param name
	 */
	public boolean connectServer(int port, String hostIp, String name) {
		// 連線伺服器
		try {
			socket = new Socket(hostIp, port);// 根據埠號和伺服器ip建立連線
			writer = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket
					.getInputStream()));
			// 傳送客戶端使用者基本資訊(使用者名稱和ip地址)
			sendMessage(name + "@" + socket.getLocalAddress().toString());
			// 開啟接收訊息的執行緒
			messageThread = new MessageThread(reader, textArea);
			messageThread.start();
			isConnected = true;// 已經連線上了
			return true;
		} catch (Exception e) {
			textArea.append("與埠號為:" + port + "    IP地址為:" + hostIp
					+ "   的伺服器連線失敗!" + "\r\n");
			isConnected = false;// 未連線上
			return false;
		}
	}

	/**
	 * 傳送訊息
	 * 
	 * @param message
	 */
	public void sendMessage(String message) {
		writer.println(message);
		writer.flush();
	}

	/**
	 * 客戶端主動關閉連線
	 */
	@SuppressWarnings("deprecation")
	public synchronized boolean closeConnection() {
		try {
			sendMessage("CLOSE");// 傳送斷開連線命令給伺服器
			messageThread.stop();// 停止接受訊息執行緒
			// 釋放資源
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

	// 不斷接收訊息的執行緒
	class MessageThread extends Thread {
		private BufferedReader reader;
		private JTextArea textArea;

		// 接收訊息執行緒的構造方法
		public MessageThread(BufferedReader reader, JTextArea textArea) {
			this.reader = reader;
			this.textArea = textArea;
		}

		// 被動的關閉連線
		public synchronized void closeCon() throws Exception {
			// 清空使用者列表
			listModel.removeAllElements();
			// 被動的關閉連線釋放資源
			if (reader != null) {
				reader.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (socket != null) {
				socket.close();
			}
			isConnected = false;// 修改狀態為斷開
		}

		public void run() {
			String message = "";
			while (true) {
				try {
					message = reader.readLine();
					StringTokenizer stringTokenizer = new StringTokenizer(
							message, "/@");
					String command = stringTokenizer.nextToken();// 命令
					if (command.equals("CLOSE"))// 伺服器已關閉命令
					{
						textArea.append("伺服器已關閉!\r\n");
						closeCon();// 被動的關閉連線
						return;// 結束執行緒
					} else if (command.equals("ADD")) {// 有使用者上線更新線上列表
						String username = "";
						String userIp = "";
						if ((username = stringTokenizer.nextToken()) != null
								&& (userIp = stringTokenizer.nextToken()) != null) {
							User user = new User(username, userIp);
							onLineUsers.put(username, user);
							listModel.addElement(username);
						}
					} else if (command.equals("DELETE")) {// 有使用者下線更新線上列表
						String username = stringTokenizer.nextToken();
						User user = (User) onLineUsers.get(username);
						onLineUsers.remove(user);
						listModel.removeElement(username);
					} else if (command.equals("USERLIST")) {// 載入線上使用者列表
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
					} else if (command.equals("MAX")) {// 人數已達上限
						textArea.append(stringTokenizer.nextToken()
								+ stringTokenizer.nextToken() + "\r\n");
						closeCon();// 被動的關閉連線
						JOptionPane.showMessageDialog(frame, "伺服器緩衝區已滿!", "錯誤",
								JOptionPane.ERROR_MESSAGE);
						return;// 結束執行緒
					} else {// 普通訊息
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
