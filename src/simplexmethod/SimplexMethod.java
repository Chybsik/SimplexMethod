/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simplexmethod;

import java.util.Arrays;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Timur
 */
public class SimplexMethod {

    /**
     * @param args the command line arguments
     * @param maximization нужно ли максимизировать или минимизировать функцию
     * @param M
     *
     * @param matrix вычисления
     * @param matrixTemp для хранения предыдущых вычислений матрицы
     * @param inequalitiesSigns знаки неравенств (равенств)
     * @param b правые части
     * @param bTemp для хранения предыдущих вычислений правой части
     * @param func функция
     * @param z вычисленые симплексные разности
     * @param Cb значения базиса
     * @param paramIndices индексы базиса
     * @param solution конечное решение
     */
    byte maximization;
    final float M = 8192;
    int x;
    int y;
    float[][] matrix;
    float[][] matrixTemp;
    byte[] inequalitiesSigns;
    float[] b;
    float[] bTemp;
    float[] func;
    float[] funcDual;
    float[] z;
    float[] Cb;
    int[] basisIndices;
    boolean exit;
    boolean unlimited;

    int pColumn;
    int pLine;

    int artificialCount;
    int balanceCount;

    int iterations = 0;
    //float optimal = 0;
    //float[] solution;

    public static void main(String[] args) throws IOException {
        // TODO code application logic here
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        SimplexMethod object = new SimplexMethod();
        try {
            System.out.println("Введите кол-во переменных");
            object.x = Integer.parseInt(br.readLine());
            System.out.println("Введите кол-во (не)равенств");
            object.y = Integer.parseInt(br.readLine());

            object.matrix = new float[object.x + object.y * 2][object.y];
            object.matrixTemp = new float[object.x + object.y * 2][object.y];
            object.inequalitiesSigns = new byte[object.y];
            object.b = new float[object.y];
            
            object.bTemp = new float[object.y];
            object.func = new float[object.x + object.y * 2];
            object.funcDual = new float[object.y];
            object.z = new float[object.x + object.y * 2];
            object.Cb = new float[object.y];
            object.basisIndices = new int[object.y];

            object.artificialCount = object.y;
            object.balanceCount = object.y;

            

            java.util.Arrays.fill(object.func, 0);
            java.util.Arrays.fill(object.z, 0);
            java.util.Arrays.fill(object.basisIndices, 0);

            System.out.println("Введите уравнение функции");
            String[] temp = br.readLine().split(" ");

            object.maximization = ("max".equals(temp[0])) ? (byte) 1 : (byte) -1;
            for (int i = 0; i < object.x; i++) {
                object.func[i] = Float.parseFloat(temp[i + 2]);
                object.z[i] = Float.parseFloat(temp[i + 2]);
            }

            System.out.println("Введите уравнения(неравенства)");
            for (int i = 0; i < object.y; i++) {

                temp = br.readLine().split(" ");
                for (int j = 0; j < object.x; j++) {
                    object.matrix[j][i] = Float.parseFloat(temp[j]);
                }
                switch (temp[object.x]) {
                    case "=":
                        object.inequalitiesSigns[i] = 0;
                        break;
                    case ">":
                        object.inequalitiesSigns[i] = 1;
                        break;
                    case "<":
                        object.inequalitiesSigns[i] = -1;
                        break;
                }
                object.b[i] = Float.parseFloat(temp[object.x + 1]);
                object.funcDual[i]=object.b[i];
                object.bTemp[i] = Float.parseFloat(temp[object.x + 1]);
            }

        } catch (NumberFormatException nfe) {
            System.err.println("Invalid Format!");
        }
        //System.out.println("Введите ограничение");
        //object.br.readLine().split(" ");
        System.out.println(object.solve());
        //System.out.println("итерации = " + object.iterations);
        System.out.println("Таблица последней итерации: ");
        for (int j = 0; j < object.y; j++) {
            String tempOut="";
            for (int i = 0; i < object.matrix.length; i++) {
                tempOut+=object.matrix[i][j]+" ";
            }
            System.out.println(tempOut);
        }
        String tempOut ="";
        for (int i = 0; i < object.z.length; i++) {
            tempOut+=object.z[i]+", ";
        }
        System.out.println(tempOut);
    }

    public String solve() {
        //Проверка на отрицательную правую часть
        for (int i = 0; i < y; i++) {
            if (b[i] < 0) {
                b[i] *= -1;
                inequalitiesSigns[i] *= -1;
                for (int j = 0; j < x; j++) {
                    matrix[j][i] *= -1;
                }
            }
        }

        //Заполнение матрицы балансовыми и искусственными
        for (int i = 0; i < inequalitiesSigns.length; i++) {
            matrix[x + i][i] = inequalitiesSigns[i] * -1; // балансовые
            matrix[x + y + i][i] = inequalitiesSigns[i] < 0 ? 0 : 1; // искусственные
            Cb[i] = inequalitiesSigns[i] < 0 ? 0 : M * -maximization;
            func[x + y + i] = inequalitiesSigns[i] < 0 ? 0 : M * -maximization;
        }

        //заполнение базисных индексов
        int s = 0;
        for (int i = x; i < x + y * 2; i++) {

            if (isBelonging(matrix[i], 1)) {
                basisIndices[s] = i;
                s++;
            }
        }

        //Сдвиг балансовых и искусственых
        for (int i = x; i < x + y * 2; i++) {
            if (!isBelonging(matrix[i], 1) && !isBelonging(matrix[i], -1)) {
                if (i < x + y) {
                    balanceCount--;
                } else {
                    artificialCount--;
                }
            }
        }
        for (int i = x; i < x + y * 2 - 1; i++) {
            if (!isBelonging(matrix[i], 1) && !isBelonging(matrix[i], -1)) {
                for (int j = i + 1; j < x + y * 2; j++) {
                    if (isBelonging(matrix[j], 1) || isBelonging(matrix[j], -1)) {
                        matrix[i] = matrix[j].clone();
                        java.util.Arrays.fill(matrix[j], 0);
                        func[i] = func[j];
                        func[j] = 0;
                        z[i] = z[j];
                        z[j] = 0;
                        break;
                    }
                }
            }
        }

        matrix = shrinkArray(matrix);
        matrixTemp = shrinkArray(matrixTemp);
        func = shrinkArray(func);
        z = shrinkArray(z);
        
        pColumn = 0;
        pLine = 0;

        zCompute();

        iteration(false);

        float[] solution = new float[x + balanceCount];
        float optimal =0;
        
        for (int i = 0; i < y; i++) {
            solution[basisIndices[i]] = b[i];
            optimal += b[i] * Cb[i];
        }
        String out = "Zопт = " + optimal + ", minF(";
        for (int i = 0; i < x + y; i++) {
            out += solution[i] + ",";
        }
        out += ")";
        out+="\nfuncDual(";
        float temp2 = 0;
        for (int i = 0; i < y; i++) {
            out+=z[x+i];
            out+=",";
            temp2+=z[x+i]*funcDual[i];
        }
        out+=") zDual=";
        out +=temp2;
        
        return out;
    }

    public void iteration(boolean singleIteration) {
        iterations++;
        for (int i = 0; i < x + balanceCount + artificialCount; i++) {
            for (int j = 0; j < y; j++) {
                matrixTemp[i][j] = matrix[i][j];
            }
        }

        basisIndices[pLine] = pColumn;
        Cb[pLine] = func[pColumn];

        //Расчет
        for (int i = 0; i < x + balanceCount; i++) {
            for (int j = 0; j < y; j++) {
                if (j == pLine && i == pColumn) {
                    matrix[i][j] = 1;
                } else if (j == pLine) {
                    matrix[i][j] /= matrixTemp[pColumn][pLine];

                } else if (i == pColumn) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] -= matrixTemp[pColumn][j] * matrixTemp[i][pLine] / matrixTemp[pColumn][pLine];
                }
            }
        }
        bTemp = b.clone();
        for (int i = 0; i < y; i++) {
            if (i == pLine) {
                b[i] /= matrixTemp[pColumn][pLine];
            } else {
                b[i] -= matrixTemp[pColumn][i] * bTemp[pLine] / matrixTemp[pColumn][pLine];
            }
        }

        zCompute();

        if (singleIteration) {
            return;
        }
        
        if (exit || iterations==1000) {

            boolean isBasisSimDifsContainsZero = false;
            boolean isSimDifsContainsZero = false;
            int temp = 0;

            for (int i = 0; i < z.length; i++) {
                if (z[i] == 0) {
                    if (isBelonging(basisIndices, i)) {
                        isBasisSimDifsContainsZero = true;
                    } else {
                        isSimDifsContainsZero = true;
                        if (i<x+balanceCount) {
                            temp = i;
                        }
                    }
                }
            }
            if (isSimDifsContainsZero && isBasisSimDifsContainsZero) {
                System.err.println("Имеется альтернативный оптимум");
                singleIteration = true;
                
                pColumn = temp;

                calcPLine();

                float[] solution = new float[x + y];
                float optimal =0;
                for (int i = 0; i < y; i++) {
                    solution[basisIndices[i]] = b[i];
                    optimal += b[i] * Cb[i];
                }
                String out = "Zопт = " + optimal + ", minF(";
                for (int i = 0; i < x + y; i++) {
                    out += solution[i] + ",";
                }
                out += ")";
                
                System.out.println(out);

            } else {
                return;
            }
        }
        iteration(singleIteration);
    }

    void zCompute() {
        //Расчет сиплексных разностей и нахождение направляющего элемента
        exit = true;
        //Направляющий столбец
        float max = maximization == 1 ? M : -M;
        float simDif = 0;

        for (int i = 0; i < x + balanceCount; i++) {

            for (int j = 0; j < y; j++) {
                simDif += matrix[i][j] * Cb[j];
                exit &= Cb[j] != M;
            }
            simDif -= func[i];

            //Проверка на неограниченость функции
            if (isBelonging(basisIndices,i)) {
                if (simDif <= 0) {
                unlimited = true;
                for (int j = 0; j < y; j++) {
                    unlimited &= matrix[i][j] <= 0;
                }
                if (unlimited) {
                    System.err.println("Функция не ограничена сверху");
                    System.exit(0);
                    //return;
                }
            }
            }
            

            //Направляющий столбец
            if (maximization == 1) {
                if (simDif < max) {
                    max = simDif;
                    pColumn = i;
                }
                exit &= !(simDif < 0);
            } else {
                if (simDif > max) {
                    max = simDif;
                    pColumn = i;
                }
                exit &= !(simDif > 0);
            }

            z[i] = simDif;
            simDif = 0;
        }
        calcPLine();
    }

    //Поиск напраляющей строки
    void calcPLine(){
        float min = M;
        float quitient = 0;

        for (int i = 0; i < y; i++) {
            if ((quitient = b[i] / matrix[pColumn][i]) < min) {
                if (!(quitient < 0)) {
                    min = quitient;
                    pLine = i;
                }
            }
        }
    }
    
    boolean isBelonging(int[] a, int x) {
        boolean result = false;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] == x;
        }
        return result;
    }

    boolean isBelonging(float[] a, int x) {
        boolean result = false;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] == (float) x;
        }
        return result;
    }

    float[] shrinkArray(float[] a) {
        float[] result = new float[x + balanceCount + artificialCount];
        for (int i = 0; i < result.length; i++) {
            result[i] = a[i];
        }
        return result;
    }

    float[][] shrinkArray(float[][] a) {
        float[][] result = new float[x + balanceCount + artificialCount][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new float[a[0].length];
            for (int j = 0; j < result[i].length; j++) {
                result[i][j] = a[i][j];
            }
        }
        return result;
    }
}
