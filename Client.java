package screen_share;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/**
 * 远程监控客户端
 * Author:tang
 * Date:2022/9/20 14:08
 */
public class Client {
    public static void main(String[] args) {
        //确认是否连接
        int dialog = JOptionPane.showConfirmDialog(null, "是否确认监控", "sugar-monitoring",JOptionPane.YES_NO_CANCEL_OPTION);
        if(dialog==JOptionPane.CANCEL_OPTION || dialog==JOptionPane.NO_OPTION) return;
        String newSocket = JOptionPane.showInputDialog("请输入ip和端口号", "192.168.43.85:10001");
        //获取ip端口号
        String ip = newSocket.substring(0, newSocket.indexOf(":"));
        String port = newSocket.substring(newSocket.indexOf(":") + 1);
        try {
            //创建连接套接字socket
            Socket socket = new Socket(ip,Integer.parseInt(port));
            InputStream inputStream = socket.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            //读取并重新设置分辨率
            double width = dataInputStream.readDouble();
            double height = dataInputStream.readDouble();
            Dimension dimension = new Dimension((int)width,(int)height);
            //定义监控窗口
            JFrame jFrame=new JFrame();
            JLabel jLabel=new JLabel();
            //设置窗口属性
            setJFrame(jFrame,jLabel,dimension);
            //开启监控画面线程
            ClientMonitoringThread clientMonitoringThread = new ClientMonitoringThread(dataInputStream,jLabel,jFrame);
            clientMonitoringThread.start();
            //绑定鼠标事件
            ClientMouseEvent clientMouseEvent = new ClientMouseEvent(dataOutputStream,jLabel,jFrame);
            //绑定键盘事件
            ClientKeyboardEvent clientKeyBoardEvent = new ClientKeyboardEvent(ip, 10002, jFrame, jLabel);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 设置窗口属性
     * @param jFrame
     * @param jLabel
     * @param dimension
     */
    public static void setJFrame(JFrame jFrame,JLabel  jLabel,Dimension dimension){
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setSize(600,600);
        jFrame.setTitle("糖糖的监控窗口");
        jFrame.setSize(dimension);
        //添加面板
        JPanel jPanel=new JPanel();
        //设置滚动条
        JScrollPane jScrollPane=new JScrollPane(jPanel);
        jPanel.setLayout(new FlowLayout());
        jPanel.add(jLabel);
        jFrame.add(jScrollPane);
        //设置窗口属性
        jFrame.setLocationRelativeTo(null);
        jFrame.setAlwaysOnTop(true);
        jFrame.setVisible(true);
    }

}

/**
 *  监控线程
 * 获取画面
 */
class ClientMonitoringThread extends Thread{
    private DataInputStream dataInputStream;
    private JLabel jLabel;
    private JFrame  jFrame;
    public ClientMonitoringThread(DataInputStream dataInputStream, JLabel jLabel, JFrame  jFrame){
        this.dataInputStream=dataInputStream;
        this.jLabel=jLabel;
        this.jFrame=jFrame;
    }

    @Override
    public void run() {
        while (true){
            int len= 0;
            try {
                len = dataInputStream.readInt();
                byte[] bytes = new byte[len];
                dataInputStream.readFully(bytes);
                ImageIcon image=new ImageIcon(bytes);
                jLabel.setIcon(image);
                jFrame.repaint();//重绘画面
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}

/**
 * 绑定客户端的鼠标事件
 */
class ClientMouseEvent {
    private DataOutputStream dataOutputStream;
    private JLabel jLabel;
    private JFrame  jFrame;
    public ClientMouseEvent(DataOutputStream dataOutputStream, JLabel jLabel, JFrame  jFrame){
        this.dataOutputStream=dataOutputStream;
        this.jLabel=jLabel;
        this.jFrame=jFrame;
        addMouseMoveEvent();
    }
    private void addMouseMoveEvent(){
        MouseListener mouseListener=new MouseAdapter() {

            /**
             * 鼠标点击
             * @param e
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if(e.getClickCount()==1 && e.getButton()==1){
                        dataOutputStream.writeInt(-11);
                    }
                    dataOutputStream.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            /**
             * 鼠标按下
             * @param e
             */
            @Override
            public void mousePressed(MouseEvent e) {
                try {
                    if(e.getButton()==1){
                        dataOutputStream.writeInt(-12);
                        dataOutputStream.writeInt(e.getX()+1);
                        dataOutputStream.writeInt(e.getY());
                        dataOutputStream.writeInt(e.getX()-1);
                        dataOutputStream.writeInt(e.getY());
                    }else if(e.getButton()==3) {
                        dataOutputStream.writeInt(-13);
                        /**
                         * 滚轮滑动
                         * @param e
                         */

                    }
                    dataOutputStream.flush();
                } catch (Exception Exception) {
                    Exception.printStackTrace();
                }
            }

            /**
             * 鼠标释放
             * @param e
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                if(e.getButton()==1){
                    dataOutputStream.writeInt(-21);
                }else if(e.getButton()==3) {
                    dataOutputStream.writeInt(-31);
                }
                    dataOutputStream.flush();
                } catch (Exception Exception) {
                    Exception.printStackTrace();
                }
            }

            /**
             * 鼠标进入
             * @param e
             */
            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
            }

            /**
             * 鼠标移出
             * @param e
             */
            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
            }
        };
        MouseMotionListener mouseMotionListener = new MouseMotionListener() {
            /**
             * 鼠标拖拽
             * @param e
             */
            @Override
            public void mouseDragged(MouseEvent e) {
                try {
                    dataOutputStream.writeInt(-1);
                    dataOutputStream.writeInt(e.getX());
                    dataOutputStream.writeInt(e.getY());
                    dataOutputStream.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            }

            /**
             * 鼠标移动
             * @param e
             */
            @Override
            public void mouseMoved(MouseEvent e) {
                try {
                    dataOutputStream.writeInt(e.getX());
                    dataOutputStream.writeInt(e.getY());
                    dataOutputStream.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        };
        MouseWheelListener mouseWheelListener = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                try {
                    if(e.getWheelRotation()==1){
                        dataOutputStream.writeInt(-6);
                    }else {
                        dataOutputStream.writeInt(-9);
                    }
                    dataOutputStream.flush();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        jLabel.addMouseWheelListener(mouseWheelListener);
        jLabel.addMouseMotionListener(mouseMotionListener);
        jLabel.addMouseListener(mouseListener);
    }

}

/**
 * 监听键盘事件
 */
class ClientKeyboardEvent {
    private String ip;
    private int port;
    private Socket socket;
    private JFrame jFrame;
    private JLabel jLabel;
    private DataOutputStream dataOutputStream;

    public ClientKeyboardEvent(String ip, int port, JFrame jFrame, JLabel jLabel) {
        this.ip = ip;
        this.port = port;
        this.jFrame=jFrame;
        this.jLabel=jLabel;
       getDataOutputStream();
       addKeyboardEvent();
    }

    private void getDataOutputStream(){
        try {
            this.socket=new Socket(ip,port);
            OutputStream outputStream = socket.getOutputStream();
            this.dataOutputStream=new DataOutputStream(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void addKeyboardEvent(){
        KeyListener keyListener=new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                try {
                    dataOutputStream.writeInt(-1);
                    dataOutputStream.writeInt(e.getKeyCode());
                    dataOutputStream.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    dataOutputStream.writeInt(-2);
                    dataOutputStream.writeInt(e.getKeyCode());
                    dataOutputStream.flush();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

        };
        jFrame.addKeyListener(keyListener);
    }


}
