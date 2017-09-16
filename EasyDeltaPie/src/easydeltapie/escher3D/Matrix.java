/*
 * Copyright (C) 2017 Escher3D
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package easydeltapie.escher3D;

/**
 *
 * @author Escher3D
 * @author LucidWolf <https://github.com/LucidWolf>
 */
public class Matrix {
    double data[][];
    public Matrix(int rows, int col){
        data = new double[rows][col];
    }
    public void swapRows(int i, int j, int numCols) {
	if (i != j) {
            for (int k = 0; k < numCols; ++k) {
                    double temp = this.data[i][k];
                    this.data[i][k] = this.data[j][k];
                    this.data[j][k] = temp;
            }
	}
    }
// Perform Gauus-Jordan elimination on a matrix with numRows rows and (njumRows + 1) columns
    public double[] gaussJordan(int numRows) {
        double solution[] = new double[numRows];
	for (int i = 0; i < numRows; ++i) {
		// Swap the rows around for stable Gauss-Jordan elimination
		double vmax = Math.abs(this.data[i][i]);
		for (int j = i + 1; j < numRows; ++j) {
			double rmax = Math.abs(this.data[j][i]);
			if (rmax > vmax) {
				this.swapRows(i, j, numRows + 1);
				vmax = rmax;
			}
		}

		// Use row i to eliminate the ith element from previous and subsequent rows
		double v = this.data[i][i];
		for (int j = 0; j < i; ++j) {
			double factor = this.data[j][i]/v;
			this.data[j][i] = 0.0;
			for (int k = i + 1; k <= numRows; ++k) {
				this.data[j][k] -= this.data[i][k] * factor;
			}
		}

		for (int j = i + 1; j < numRows; ++j) {
			double factor = this.data[j][i]/v;
			this.data[j][i] = 0.0;
			for (int k = i + 1; k <= numRows; ++k) {
				this.data[j][k] -= this.data[i][k] * factor;
			}
		}
	}

	for (int i = 0; i < numRows; ++i) {
		solution[i] = (this.data[i][numRows] / this.data[i][i]);
	}
        return solution;
}


}
