import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * Demo of the ColorGrid and a heat map
 */
public class CGDemo {
	private final int DIM = 2;
	private final String REPLAY = "Replay";
	private JFrame application;
	private JButton button;
	private Color[][] grid;
	private ArrayList<int[][]> tallyList;

	public CGDemo(ArrayList<int[][]> tallyList) {
		this.tallyList = tallyList;

		grid = new Color[DIM][DIM];
		application = new JFrame();
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fillGrid(grid, 0);

		ColoredGrid gridPanel = new ColoredGrid(grid);
		application.add(gridPanel, BorderLayout.CENTER);

		button = new JButton(REPLAY);
		button.addActionListener(new BHandler());
		application.add(button, BorderLayout.PAGE_END);

		application.setSize(DIM * 280, (int) (DIM * 300));
	}

	public void displayHeatmap() throws FileNotFoundException, InterruptedException {
		application.setVisible(true);
		application.repaint();
		animate();
	}

	private void animate() throws InterruptedException {
		button.setEnabled(false);
		for (int i = 0; i < tallyList.size(); i++) {
			for (int j = 0; j < DIM; j++) {
				fillGrid(grid, i);
				application.repaint();
				Thread.sleep(50);
			}
			button.setEnabled(true);
			application.repaint();
		}
	}

	class BHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (REPLAY.equals(e.getActionCommand())) {
				new Thread() {
					public void run() {
						try {
							animate();
						} catch (InterruptedException e) {
							System.exit(0);
						}
					}
				}.start();
			}
		}
	};

	private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
	private int offset = 0;

	private void fillGrid(Color[][] grid, int i) {
		int[][] tally = tallyList.get(i);

		for (int r = 0; r < grid.length; r++)
			for (int c = 0; c < grid[r].length; c++) {
				int tallyNum = tally[r][c];
				double ratio = tallyNum / 50.0; //for calculating color
				grid[r][c] = interpolateColor(ratio, COLD, HOT);
			}
		offset += DIM;
	}

	private Color interpolateColor(double ratio, Color a, Color b) {
		int ax = a.getRed();
		int ay = a.getGreen();
		int az = a.getBlue();
		int cx = ax + (int) ((b.getRed() - ax) * ratio);
		int cy = ay + (int) ((b.getGreen() - ay) * ratio);
		int cz = az + (int) ((b.getBlue() - az) * ratio);
		return new Color(cx, cy, cz);
	}
}