package com.lvrenyang.myactivity;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.lvrenyang.R;
import com.lvrenyang.myprinter.WorkService;
import com.lvrenyang.myprinter.Global;
import com.lvrenyang.utils.DataUtils;
import com.lvrenyang.utils.TimeUtils;

public class ListenBTActivity extends Activity implements OnClickListener {

	private ProgressDialog dialog;

	private static Handler mHandler = null;
	private static String TAG = "ListenBTActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_listenbt);

		findViewById(R.id.buttonListen).setOnClickListener(this);
		dialog = new ProgressDialog(this);

		mHandler = new MHandler(this);
		WorkService.addHandler(mHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WorkService.delHandler(mHandler);
		mHandler = null;
	}

	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {
		case R.id.buttonListen: {
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if (null == adapter) {
				finish();
				break;
			}

			if (!adapter.isEnabled()) {
				if (adapter.enable()) {
					while (!adapter.isEnabled())
						;
					Log.v(TAG, "Enable BluetoothAdapter");
				} else {
					finish();
					break;
				}
			}
			
			if(null != WorkService.workThread)
			{
				WorkService.workThread.disconnectBt();
				TimeUtils.WaitMs(50);
			}

			WorkService.workThread.disconnectBt();
			// 只有没有连接且没有在用，这个才能改变状态
			dialog.setMessage("Waiting...");
			dialog.setIndeterminate(true);
			dialog.setCancelable(false);
			dialog.show();
			WorkService.workThread.connectBtAsServer("", 1000*30);
			
			break;
		}
		}
	}

	static class MHandler extends Handler {

		WeakReference<ListenBTActivity> mActivity;

		MHandler(ListenBTActivity activity) {
			mActivity = new WeakReference<ListenBTActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			ListenBTActivity theActivity = mActivity.get();
			switch (msg.what) {
			/**
			 * DrawerService 的 onStartCommand会发送这个消息
			 */

			case Global.MSG_WORKTHREAD_SEND_CONNECTBTSRESULT: {
				int result = msg.arg1;
				Toast.makeText(
						theActivity,
						(result == 1) ? Global.toast_success
								: Global.toast_fail, Toast.LENGTH_SHORT).show();
				Log.v(TAG, "Connect Result: " + result);
				theActivity.dialog.cancel();
				if (1 == result) {
					PrintTest();
				}
				break;
			}

			}
		}

		void PrintTest() {
			String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789\n";
			byte[] tmp1 = { 0x1b, 0x40, (byte) 0xB2, (byte) 0xE2, (byte) 0xCA,
					(byte) 0xD4, (byte) 0xD2, (byte) 0xB3, 0x0A };
			byte[] tmp2 = { 0x1b, 0x21, 0x01 };
			byte[] tmp3 = { 0x0A, 0x0A, 0x0A, 0x0A };
			byte[] buf = DataUtils.byteArraysToBytes(new byte[][] { tmp1,
					str.getBytes(), tmp2, str.getBytes(), tmp3 });
			if (WorkService.workThread.isConnected()) {
				Bundle data = new Bundle();
				data.putByteArray(Global.BYTESPARA1, buf);
				data.putInt(Global.INTPARA1, 0);
				data.putInt(Global.INTPARA2, buf.length);
				WorkService.workThread.handleCmd(Global.CMD_WRITE, data);
			} else {
				Toast.makeText(mActivity.get(), Global.toast_notconnect,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

}
