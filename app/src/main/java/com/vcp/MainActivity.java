package com.vcp;

import android.app.Dialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tipTv;
    private TextView costTimeTv;
    private TextView buyTimesTv;
    private TextView buyAmountTv;
    private TextView bobusAmountTv;
    private TextView bobusDetailTv;

    /**
     * 彩奖 ，6个本期蓝球号码
     */
    private List<Integer> bonusList = new ArrayList<>();

    /**
     * 红彩， 1个本期红球号码
     */
    private int bonusRed;

    /**
     * 中奖总奖金
     */
    private long bonusAmount;

    /**
     * 每一注的中奖级别，6级最高奖
     */
    private static int bonus1, bonus2, bonus3, bonus4, bonus5, bonus6;
    /**
     * 未中奖的级别，3个蓝球及以下并且无红球，不中奖；
     */
    private static int noBonusTimes;

    public static ExecutorService newFixThreadPool(int nThreads){
        return new ThreadPoolExecutor(nThreads, nThreads, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    private ExecutorService pool = newFixThreadPool(1);

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    if (message.obj != null && message.obj instanceof List) {
                        List<Integer> blue = (List<Integer>) message.obj;
                        int red = blue.get(blue.size() - 1);
                        blue.remove(blue.size() - 1);
                        StringBuilder sb = new StringBuilder();
                        for (int i : blue) {
                            sb.append("  ").append(i);
                        }
                        sb.append("  /  ").append(red);
                        tipTv.setText(sb.toString());
                    }
                    break;
                case 1:
                    loopRand(1, 0);
                    break;
                case 2:
                    loopRand(10000, 0);
                    break;
                case 3:
                    loopRand(0, 5);
                    break;
                case 4:
                    loopRand(0, 6);
                    break;
                case 5:
                    if (message.obj != null && message.obj instanceof Map) {
                        Map<String, String> map = (Map<String, String>) message.obj;
                        if (map != null) {
                            costTimeTv.setText(map.get("Cost"));
                            buyTimesTv.setText(map.get("Turn"));
                            buyAmountTv.setText(map.get("Spend"));
                            bobusAmountTv.setText(map.get("Bonus"));
                            bobusDetailTv.setText(map.get("Detail"));
                            clearCollectTimes();
                        }
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView blueNum = findViewById(R.id.tv_bonus_num_blue);
        TextView redNum = findViewById(R.id.tv_bonus_num_red);
        tipTv = findViewById(R.id.tv_tip);
        costTimeTv = findViewById(R.id.tv_cost_time);
        buyTimesTv = findViewById(R.id.tv_buy_times);
        buyAmountTv = findViewById(R.id.tv_buy_amount);
        bobusAmountTv = findViewById(R.id.tv_bonus_amount);
        bobusDetailTv = findViewById(R.id.tv_bonus_detail);

        findViewById(R.id.tv_select_robot).setOnClickListener(this);
        findViewById(R.id.tv_select_robot_wan).setOnClickListener(this);
        findViewById(R.id.tv_select_till_five).setOnClickListener(this);
        findViewById(R.id.tv_select_till_ten).setOnClickListener(this);

        /**
         * 随机出一组6个有序不重复数字组，当做本期号码
         */
        bonusList = randomCaipiao(33, 6);
        StringBuilder str = new StringBuilder();
        for (int i : bonusList) {
            str.append("  ").append(i);
        }
        blueNum.setText(str.toString());
        /**
         * 随机一个特殊号码，当做本期红球
         */
        bonusRed = new Random().nextInt(16) + 1;
        redNum.setText(String.valueOf(bonusRed));

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_select_robot:
                // 机选一次
                if (loopStopFlag) {
                    mHandler.sendEmptyMessage(1);
                } else {
                    Toast.makeText(this, "请等待投完1注！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tv_select_robot_wan:
                // 机选1万次
                if (loopStopFlag) {
                    tipTv.setText("统计1万次投注的预期结果（一注单价￥2.00）");
                    mHandler.sendEmptyMessage(2);
                } else {
                    Toast.makeText(this, "请等待投完1万注！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tv_select_till_five:
                // 机选，直到中500万（6蓝全中）
                if (loopStopFlag) {
                    tipTv.setText("机选 - 直到中了500万或者1000万大奖！");
                    mHandler.sendEmptyMessage(3);
                } else {
                    Toast.makeText(this, "请等待中奖500万！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.tv_select_till_ten:
                // 机选，直到中1000万（6蓝1红全中）
                if (loopStopFlag) {
                    tipTv.setText("机选 - 直到中了1000万最高奖！");
                    mHandler.sendEmptyMessage(4);
                } else {
                    Toast.makeText(this, "请等待中奖1000万！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private static boolean loopStopFlag = true;

    /**
     * 子线程执行循环操作，循环条件由外部标记和内部条件构成。
     * 子线程中的循环要用外部标记来停止
     *
     * @param loopCount 循环次数，如果有循环次数，循环天骄就根据次数来判断
     * @param level     循环次数<=0，level才可以作为判断条件，值取6：循环出一等奖，值取5：循环出二等奖以上
     */
    private void loopRand(final int loopCount, final int level) {
        final int loopTime = loopCount;
        showLoadingDialog(true);
        clearCollectTimes();
        pool.execute(new Runnable() {
            @Override
            public void run() {
                loopStopFlag = false;
                long time = System.currentTimeMillis();
                int turn = 0;
                boolean big = false;
                boolean condition;
                if (loopTime > 0) {
                    // 有固定投注次数
                    condition = turn < loopTime;
                } else {
                    condition = !big;
                }
                while (!loopStopFlag && condition) {
                    turn++;
                    List<Integer> list = randomCaipiao(33, 6);
                    int type = getBonusType(list);
                    int redInt = new Random().nextInt(16) + 1;
                    collectBonus(type, redInt == bonusRed);
                    if (loopTime > 0) {
                        condition = turn < loopTime;
                        if (loopTime == 1 && !condition) {
                            list.add(redInt);
                            Message m = Message.obtain();
                            m.what = 0;
                            m.obj = list;
                            mHandler.sendMessage(m);
                        }
                    } else if (level == 6 && bonus6 > 0) {
                        // bonus6大于0，说明中了一次1000万
                        big = true;
                        condition = !big;
                    } else if (level == 5 && (bonus5 > 0 || bonus6 > 0)) {
                        // 5, 6有值，说明最少500万
                        big = true;
                        condition = !big;
                    }
                }
                long costTime = System.currentTimeMillis() - time;

                Message m = Message.obtain();
                m.what = 5;
                Map<String, String > map = new HashMap<>();
                map.put("Cost", String.valueOf(costTime + " ms"));
                map.put("Turn", String.valueOf(turn + " 次"));
                map.put("Spend", String.valueOf(turn * 2 + ".00"));
                map.put("Bonus", String.valueOf(getBonusAmount() + ".00"));
                map.put("Detail", showDetail());
                m.obj = map;
                mHandler.sendMessage(m);
                loopStopFlag = true;
            }
        });
        showLoadingDialog(false);
    }

    /**
     * 随机出一串双色球
     */
    private List<Integer> randomCaipiao(int total, int count) {
        // 总数33个篮球
        int[] arr = new int[total];
        // 随机出的结果，6蓝
        List<Integer> cp = new ArrayList<>();
        for (int x = 0; x < total; x++) {
            arr[x] = x;
        }
        for (int y = 0; y < count; y++) {
            // 随机出角标y+1~32的数字。和角标y比较
            int t = randSelect(y, total - 1);
            // 交换
            arr[y] = arr[y] ^ arr[t];
            arr[t] = arr[y] ^ arr[t];
            arr[y] = arr[y] ^ arr[t];
            // 取值。+1保证是1-33
            cp.add(arr[y] + 1);
        }
        // 排序数组
        Collections.sort(cp);
        return cp;
    }

    /**
     * 从整数区间选一个随机数，包含头不包含尾
     * 例：20~30 --> t=0-9, return 20+t : ( 20-29 )
     */
    private int randSelect(int n, int m) {
        int t = new Random().nextInt(Math.abs(m - n)) + 1;
        if (n > m) {
            return t + m;
        } else {
            return t + n;
        }
    }

    /**
     * 获取中奖类型 返回的值代表有几个中的数字
     *
     * @param list 蓝数
     * @return 中了几个蓝球数
     */
    private int getBonusType(List<Integer> list) {
        int cnt = 0;
        if (list != null && bonusList != null) {
            for (int x : bonusList) {
                if (list.contains(x)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

    /**
     * 计算总奖金：1等奖1千万，2等奖500万，3等奖3千，4等奖200， 5等奖10块，6等奖5块
     */
    public long getBonusAmount() {
        bonusAmount = bonus6 * 10000000 + bonus5 * 5000000 +
                bonus4 * 3000 + bonus3 * 200 + bonus2 * 10 + bonus1 * 5;
        return bonusAmount;
    }

    private void clearCollectTimes() {
        bonus1 = 0;
        bonus2 = 0;
        bonus3 = 0;
        bonus4 = 0;
        bonus5 = 0;
        bonus6 = 0;
        noBonusTimes = 0;
    }

    /**
     * 中奖详情分解
     */
    private String showDetail() {
        StringBuilder detail = new StringBuilder();
        detail.append(" 一等奖：").append(bonus6).append(" 次 \n ")
                .append("二等奖：").append(bonus5).append(" 次 \n ")
                .append("三等奖：").append(bonus4).append(" 次 \n ")
                .append("四等奖：").append(bonus3).append(" 次 \n ")
                .append("五等奖：").append(bonus2).append(" 次 \n ")
                .append("六等奖：").append(bonus1).append(" 次 \n ")
                .append("未中奖：").append(noBonusTimes).append(" 次 \n ");
        return detail.toString();
    }

    /**
     * 统计各个类型中奖数目
     *
     * @param type   中奖级别
     * @param getRed 红球数
     */
    private void collectBonus(int type, boolean getRed) {
        if (type == 6) {
            if (getRed) {
                bonus6++;
            } else {
                bonus5++;
            }
        } else if (type == 5) {
            if (getRed) {
                bonus4++;
            } else {
                bonus3++;
            }
        } else if (type == 4) {
            if (getRed) {
                bonus3++;
            } else {
                bonus2++;
            }
        } else if (type == 3) {
            if (getRed) {
                bonus2++;
            } else {
                // 只有3个蓝球，未中奖
                noBonusTimes++;
            }
        } else {
            if (getRed) {
                bonus1++;
            } else {
                // 未中奖
                noBonusTimes++;
            }
        }
    }


    /**
     * 加载框
     */
    private Dialog loadingDialog;

    private void showLoadingDialog(boolean flag) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(MainActivity.this, R.style.Dialog_Fullscreen);
            View layout = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
            loadingDialog.setContentView(layout);
            loadingDialog.setCanceledOnTouchOutside(false);
        }
        if (flag) {
            loadingDialog.show();
        } else {
            loadingDialog.dismiss();
        }
    }

}
