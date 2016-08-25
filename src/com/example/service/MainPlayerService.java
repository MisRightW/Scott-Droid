package com.example.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.animation.AnimationUtils;

import com.example.activity.R;
import com.example.fragment.PlayerFramgment;
import com.example.lyric.Lyric;
import com.example.lyric.LyricProcessor;
import com.example.song.Song;
import com.example.song.SongList;
import com.example.utils.MyConstant;

public class MainPlayerService extends Service {
	LocalBroadcastManager localBroadcastManager2;
	PlayerStatuUpdateBroadCastReceiver playerStatuUpdateBroadCastReceiver;
	UpdateListFragFirstViewBroadCastReceiver updateListFragFirstViewBroadCastReceiver;
	UpdatePlayerFragRebackBroadCastReceiver updatePlayerFragRebackBroadCastReceiver;
	
	HeadsetPlugReceiver headsetPlugReceiver;
	
	private AudioManager audioManager;
	
	private MyOnAudioFocusChangeListener myOnAudioFocusChangeListener;
	
	public List < Song > songs;
	private LyricProcessor mLyricProcessor;
	private List < Lyric > lrcList = new ArrayList < Lyric > ( );
	// ��ʼ���ֵ
	private int index = 0;
	
	public static MediaPlayer mediaPlayer;
	public static Equalizer equalizer;
	public static BassBoost bassBoost;
	
	int currSongPositionInList;
	public int lastPlayerPositoinInList;
	public int lastPlayerProcess;
	public int currSongLoopProcess;
	public int loopStatus;
	
	private boolean isServiceAlive;
	
	private boolean isFinshACall;
	
	// �����漰�����Ĳ���״̬�ĵط���Ҫ���Ĵ˱�ǩ�����ڸ�rapper������
	public boolean isPlaying;
	
	private Thread updatePlayerThread;
	
	public MainPlayerService ( ) {
		super ( );
	}
	
	@ Override
	public IBinder onBind ( Intent intent ) {
		return null;
	}
	
	@ Override
	public void onCreate ( ) {
		super.onCreate ( );
		if ( this.songs == null ) {
			songs = SongList.getSongData ( MainPlayerService.this );
		}
		createMediaSetting ( );
		initBroadcastReceiver ( );
		restoreLastData ( );
		sendStartBroad ( );
		
		//�������������˳��Ū���ˣ��ر���
		isFinshACall = false;
		isServiceAlive = true;
		initPlayerSeekBarThread ( );
		initPhoneListener ( );
		initSystemAudioListener ( );
	}
	
	private void createMediaSetting ( ) {
		mediaPlayer = new MediaPlayer ( );
		equalizer = new Equalizer ( 0 , mediaPlayer.getAudioSessionId ( ) );
//		if ( equalizer.getEnabled ( ) ) {
		equalizer.setEnabled ( true );
//		}
		bassBoost = new BassBoost ( 0 , mediaPlayer.getAudioSessionId ( ) );
//		if ( bassBoost.getEnabled ( ) ) {
		bassBoost.setEnabled ( true );
//		}
		//MEIZU MX5   �е�����ǿ���ܣ� С��1 û��
//		System.out.println ("bassBoost.getEnabled"+bassBoost.getEnabled ( ));
	}
	
	private void initSystemAudioListener ( ) {
		audioManager = ( AudioManager ) getApplicationContext ( ).getSystemService ( Context.AUDIO_SERVICE );
		myOnAudioFocusChangeListener = new MyOnAudioFocusChangeListener ( );
	}
	
	private void initPhoneListener ( ) {
		// �����������¼�
		TelephonyManager telManager = ( TelephonyManager ) getSystemService ( Context.TELEPHONY_SERVICE ); // ��ȡϵͳ����
		telManager.listen ( new MyPhoneStateListener ( ) , PhoneStateListener.LISTEN_CALL_STATE );
		
	}
	
	private class MyPhoneStateListener extends PhoneStateListener {
		@ Override
		public void onCallStateChanged ( int state , String incomingNumber ) {
//			System.out.println (state);
			switch ( state ) {
				case TelephonyManager.CALL_STATE_IDLE : // �һ�״̬
					if ( ( ! MainPlayerService.mediaPlayer.isPlaying ( ) ) && isFinshACall ) {
						Intent intent = new Intent ( MainPlayerService.this , com.example.service.MainPlayerService.class );
						intent.putExtra ( "type" , 1 );
						intent.putExtra ( "playOperation" , 0 );
						startService ( intent );
						isFinshACall = false;
					}
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK : // ͨ��״̬
				case TelephonyManager.CALL_STATE_RINGING : // ����״̬
					if ( MainPlayerService.mediaPlayer.isPlaying ( ) ) {
						Intent intent2 = new Intent ( MainPlayerService.this , com.example.service.MainPlayerService.class );
						intent2.putExtra ( "type" , 1 );
						intent2.putExtra ( "playOperation" , 0 );
						startService ( intent2 );
						isFinshACall = true;
					}
					break;
				default :
					break;
			}
		}
	}
	
	private void restoreLastData ( ) {
		restoreSongPref ( );
		restoreMediaPref ( );
	}
	
	private void restoreMediaPref ( ) {
		SharedPreferences sharedPreferences = getSharedPreferences ( "lastMediaEqualizerStaus" , Context.MODE_PRIVATE );
		int minEqu = equalizer.getBandLevelRange ( ) [ 0 ];
		int bandGap = equalizer.getBandLevelRange ( ) [ 1 ] - equalizer.getBandLevelRange ( ) [ 0 ];
		
		int band1 = sharedPreferences.getInt ( "band1" , ( int ) ( minEqu + bandGap * 0.95 ) );
		equalizer.setBandLevel ( ( short ) 0 , ( short ) band1 );
		
		int band2 = sharedPreferences.getInt ( "band2" , ( int ) ( minEqu + bandGap * 0.85 ) );
		equalizer.setBandLevel ( ( short ) 1 , ( short ) band2 );
		
		int band3 = sharedPreferences.getInt ( "band3" , ( int ) ( minEqu + bandGap * 0.75 ) );
		equalizer.setBandLevel ( ( short ) 2 , ( short ) band3 );
		
		int band4 = sharedPreferences.getInt ( "band4" , ( int ) ( minEqu + bandGap * 0.85 ) );
		equalizer.setBandLevel ( ( short ) 3 , ( short ) band4 );
		
		int band5 = sharedPreferences.getInt ( "band5" , ( int ) ( minEqu + bandGap * 0.95 ) );
		equalizer.setBandLevel ( ( short ) 4 , ( short ) band5 );
		
		int bassTmp = 300;
		int bassStrength = sharedPreferences.getInt ( "bassBoost" , bassTmp );
		bassBoost.setStrength ( ( short ) bassStrength );
	}
	
	private void restoreSongPref ( ) {
		SharedPreferences sharedPreferences = getSharedPreferences ( "lastPlayStaus" , Service.MODE_PRIVATE );
		if ( sharedPreferences.contains ( "lastPlayerProcess" ) ) {
			this.lastPlayerProcess = sharedPreferences.getInt ( "lastPlayerProcess" , 0 );
			this.currSongLoopProcess = this.lastPlayerProcess;
		}
		if ( sharedPreferences.contains ( "lastPlayerPositoinInList" ) ) {
			this.currSongPositionInList = sharedPreferences.getInt ( "lastPlayerPositoinInList" , 0 );
		}
		if ( sharedPreferences.contains ( "lastLoopState" ) ) {
			this.loopStatus = sharedPreferences.getInt ( "lastLoopState" , 0 );
		}
		if ( sharedPreferences.contains ( "lastisPlaying" ) ) {
			this.isPlaying = sharedPreferences.getBoolean ( "lastisPlaying" , false );
		}
	}
	
	private void sendStartBroad ( ) {
		// �ı�player��UIΪ����
		Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
		intentPause.putExtra ( "playBtnTag" , 0 );
		localBroadcastManager2.sendBroadcast ( intentPause );
		
		// ��player����songid
		Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
		intentPlayerText.putExtra ( "songId" , this.currSongPositionInList );
		localBroadcastManager2.sendBroadcast ( intentPlayerText );
		
		// ��ʵʱ���Ž��ȷ��͸�player
		Intent intentCurProcess = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_SEEKBAR_UPDATE );
		intentCurProcess.putExtra ( "positionProcess" , this.lastPlayerProcess );
		intentCurProcess.putExtra ( "duration" , ( int ) songs.get ( this.currSongPositionInList ).getDuration ( ) );
		localBroadcastManager2.sendBroadcast ( intentCurProcess );
		
		// ��loopstatus���͸�player
		Intent intentLoopStatus = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_LOOPLOGO_UPDATE );
		intentLoopStatus.putExtra ( "loopLogo" , loopStatus );
		localBroadcastManager2.sendBroadcast ( intentLoopStatus );
	}
	
	private void initPlayerSeekBarThread ( ) {
		updatePlayerThread = new Thread ( new Runnable ( ) {
			@ Override
			public void run ( ) {
				while ( isServiceAlive ) {
					try {
						Thread.sleep ( 300 );
					} catch ( InterruptedException e ) {
						e.printStackTrace ( );
					}
					// ��looplogo����player
					Intent intentLoopLogo = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_LOOPLOGO_UPDATE );
					intentLoopLogo.putExtra ( "loopLogo" , loopStatus );
					localBroadcastManager2.sendBroadcast ( intentLoopLogo );
					if ( ( mediaPlayer != null ) && ( mediaPlayer.isPlaying ( ) ) ) {
						// ��ʵʱ���Ž��ȷ��͸�player
						Intent intentSeekBar = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_SEEKBAR_UPDATE );
						int positionProcess = mediaPlayer.getCurrentPosition ( );
						currSongLoopProcess = positionProcess;
						int duration = mediaPlayer.getDuration ( );
						intentSeekBar.putExtra ( "positionProcess" , positionProcess );
						intentSeekBar.putExtra ( "duration" , duration );
						localBroadcastManager2.sendBroadcast ( intentSeekBar );
						// ʵʱ���͸�ʵ�ͬ����Ϣ
						PlayerFramgment.playerLyricView.setIndex ( lrcIndex ( ) );
					}
				}
				
			}
		} );
		updatePlayerThread.start ( );
	}
	
	@ Override
	public void onDestroy ( ) {
		isServiceAlive = false;
		if ( mediaPlayer != null ) {
			mediaPlayer.stop ( );
			mediaPlayer.release ( );
			mediaPlayer = null;
		}
		this.lastPlayerProcess = this.currSongLoopProcess;
		this.lastPlayerPositoinInList = this.currSongPositionInList;
		SharedPreferences.Editor editor = getSharedPreferences ( "lastPlayStaus" , Service.MODE_PRIVATE ).edit ( );
		editor.putInt ( "lastPlayerProcess" , this.lastPlayerProcess );
		editor.putInt ( "lastLoopState" , this.loopStatus );
		editor.putBoolean ( "lastisPlaying" , this.isPlaying );
		editor.putInt ( "lastPlayerPositoinInList" , this.lastPlayerPositoinInList );
		editor.commit ( );
		unregisterReceiver ( headsetPlugReceiver );
		audioManager.abandonAudioFocus ( myOnAudioFocusChangeListener );
	}
	
	private void initBroadcastReceiver ( ) {
		localBroadcastManager2 = LocalBroadcastManager.getInstance ( this );
		playerStatuUpdateBroadCastReceiver = new PlayerStatuUpdateBroadCastReceiver ( );
		IntentFilter filter = new IntentFilter ( );
		filter.addAction ( MyConstant.ACTION_BROADCAST_PLAYERSERVICE_PLAYER_STATU_UPDATE );
		localBroadcastManager2.registerReceiver ( playerStatuUpdateBroadCastReceiver , filter );
		
		updateListFragFirstViewBroadCastReceiver = new UpdateListFragFirstViewBroadCastReceiver ( );
		IntentFilter filter2 = new IntentFilter ( );
		filter2.addAction ( MyConstant.ACTION_BROADCAST_PLAYERSERVICE_LISTFRAG_FIRST_UPDATE );
		localBroadcastManager2.registerReceiver ( updateListFragFirstViewBroadCastReceiver , filter2 );
		
		updatePlayerFragRebackBroadCastReceiver = new UpdatePlayerFragRebackBroadCastReceiver ( );
		IntentFilter filter3 = new IntentFilter ( );
		filter3.addAction ( MyConstant.ACTION_BROADCAST_PLAYERSERVICE_PLAYERFRAG_REBACK_UPDATE );
		localBroadcastManager2.registerReceiver ( updatePlayerFragRebackBroadCastReceiver , filter3 );
		
		//����ϵͳ��������γ����,��BR��ȫ�ֵģ���Ҫ��destroyʱע��ע��
		headsetPlugReceiver = new HeadsetPlugReceiver ( );
		IntentFilter filter4 = new IntentFilter ( );
		filter4.addAction ( AudioManager.ACTION_AUDIO_BECOMING_NOISY );
		registerReceiver ( headsetPlugReceiver , filter4 );
	}
	
	/**
	 * Audio���ͨ���л� ��Ӳ������������ֱ�Ӽ��������γ��¼����ѣ������İγ��Ͳ��룬�������ֻ���ƽ�ı仯��Ȼ�󴥷�ʲôʲô�ж�
	 * ����Android��ϵͳ�㲥AudioManager.ACTION_AUDIO_BECOMING_NOISY��
	 * ��������㲥ֻ��������߶���
	 * 
	 * @author scott
	 * 
	 */
	public class HeadsetPlugReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			String action = intent.getAction ( );
			if ( AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals ( action ) ) {
				if ( MainPlayerService.mediaPlayer.isPlaying ( ) ) {
					Intent intent1 = new Intent ( MainPlayerService.this , com.example.service.MainPlayerService.class );
					intent1.putExtra ( "type" , 1 );
					intent1.putExtra ( "playOperation" , 0 );
					startService ( intent1 );
				}
			}
		}
		
	}
	
	@ Override
	public int onStartCommand ( Intent intent , int flags , int startId ) {
		// ��һ����activity������serviceʱ��������type��Ϊ���������ֵ���������ǻ��д���!
		if ( intent == null ) {
			return super.onStartCommand ( intent , flags , startId );
		}
		int type = intent.getIntExtra ( "type" , 0 );
		switch ( type ) {
			case 0 :
				int loopOperation = intent.getIntExtra ( "loopOperation" , 0 );
				this.loopStatus = loopOperation;
				break;
			case 1 :
				int playOperation = intent.getIntExtra ( "playOperation" , 0 );
				if ( playOperation == 0 ) {
					if ( mediaPlayer.isPlaying ( ) ) {
						// ��currSongLoopProcess���и�ֵֻ�����������Դ�sharedpref��ã�����ʱʵʱ��run�л�ã���ͣǰ��ֵ
						this.currSongLoopProcess = mediaPlayer.getCurrentPosition ( );
						mediaPlayer.pause ( );
						this.isPlaying = false;
						
						// �ı�player��UIΪ����
						Intent intentPlay = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
						intentPlay.putExtra ( "playBtnTag" , 0 );
						localBroadcastManager2.sendBroadcast ( intentPlay );
						
						// �ı�list��UIΪ����
						Intent intentListPlay = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
						intentListPlay.putExtra ( "listBtnTag" , 0 );
						localBroadcastManager2.sendBroadcast ( intentListPlay );
						
					} else {
						if ( ! playASong ( this.currSongPositionInList , this.currSongLoopProcess ) ) {
							break;
						}
						// �ı�player��UIΪ��ͣ
						Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
						intentPause.putExtra ( "playBtnTag" , 1 );
						localBroadcastManager2.sendBroadcast ( intentPause );
						// ��player����songid
						Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
						intentPlayerText.putExtra ( "songId" , this.currSongPositionInList );
						localBroadcastManager2.sendBroadcast ( intentPlayerText );
						
						// �ı�list��UIΪ��ͣ
						Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
						intentListPause.putExtra ( "listBtnTag" , 1 );
						localBroadcastManager2.sendBroadcast ( intentListPause );
						// ��list����songid
						Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
						intentListPlayerText.putExtra ( "songId" , this.currSongPositionInList );
						localBroadcastManager2.sendBroadcast ( intentListPlayerText );
						
					}
				} else if ( playOperation == 1 ) {// ��һ��
					int songListPosition = generateLoopPrev ( this.currSongPositionInList , this.loopStatus );
					if ( songListPosition < 0 ) {
						songListPosition = 0;
					}
					if ( ! playASong ( songListPosition , 0 ) ) {
						break;
					}
					// �ı�player��UIΪ��ͣ
					Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
					intentPause.putExtra ( "playBtnTag" , 1 );
					localBroadcastManager2.sendBroadcast ( intentPause );
					// �ı�list��UIΪ��ͣ
					Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
					intentListPause.putExtra ( "listBtnTag" , 1 );
					localBroadcastManager2.sendBroadcast ( intentListPause );
					// ��player����songid
					Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
					intentPlayerText.putExtra ( "songId" , songListPosition );
					localBroadcastManager2.sendBroadcast ( intentPlayerText );
					// ��list����songid
					Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
					intentListPlayerText.putExtra ( "songId" , this.currSongPositionInList );
					localBroadcastManager2.sendBroadcast ( intentListPlayerText );
				} else if ( playOperation == 2 ) {// ��һ��
					int songListPosition = generateLoopNext ( this.currSongPositionInList , this.loopStatus );
					if ( songListPosition > songs.size ( ) - 1 ) {
						songListPosition = songs.size ( ) - 1;
					}
					if ( ! playASong ( songListPosition , 0 ) ) {
						break;
					}
					// �ı�player��UIΪ��ͣ
					Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
					intentPause.putExtra ( "playBtnTag" , 1 );
					localBroadcastManager2.sendBroadcast ( intentPause );
					// �ı�list��UIΪ��ͣ
					Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
					intentListPause.putExtra ( "listBtnTag" , 1 );
					localBroadcastManager2.sendBroadcast ( intentListPause );
					// ��list����songid
					Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
					intentListPlayerText.putExtra ( "songId" , this.currSongPositionInList );
					localBroadcastManager2.sendBroadcast ( intentListPlayerText );
					// ��player����songid
					Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
					intentPlayerText.putExtra ( "songId" , songListPosition );
					localBroadcastManager2.sendBroadcast ( intentPlayerText );
				}
				break;
			case 2 :
				// ����listview��item�����ȥ����
				int songListPositionTemp = intent.getIntExtra ( "songListPosition" , 0 );
				if ( songListPositionTemp == currSongPositionInList ) {
					break;
				}
				if ( ! playASong ( songListPositionTemp , 0 ) ) {
					break;
				}
				// �ı�player��UIΪ��ͣ
				Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
				intentPause.putExtra ( "playBtnTag" , 1 );
				localBroadcastManager2.sendBroadcast ( intentPause );
				// ��player����songid
				Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
				intentPlayerText.putExtra ( "songId" , currSongPositionInList );
				localBroadcastManager2.sendBroadcast ( intentPlayerText );
				// �ı�list��UIΪ��ͣ
				Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
				intentListPause.putExtra ( "listBtnTag" , 1 );
				localBroadcastManager2.sendBroadcast ( intentListPause );
				// ��list����songid
				Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
				intentListPlayerText.putExtra ( "songId" , currSongPositionInList );
				localBroadcastManager2.sendBroadcast ( intentListPlayerText );
				break;
			default :
				break;
		}
		return super.onStartCommand ( intent , flags , startId );
	}
	
	private int generateLoopPrev ( int songListPosition , int loopStatus ) {
		switch ( loopStatus ) {
			case 0 :
				songListPosition -- ;
				break;
			
			case 1 :
				break;
			
			case 2 :
				songListPosition = generateRamdomNumber ( );
				break;
			
			default :
				break;
		}
		return songListPosition;
	}
	
	private int generateRamdomNumber ( ) {
		// 3�����������,��Ҫ�Ľ�
		Random randomCache1 = new Random ( );
		int temp = randomCache1.nextInt ( songs.size ( ) );
		Random randomCache2 = new Random ( temp );
		temp = randomCache2.nextInt ( songs.size ( ) );
		Random randomCache3 = new Random ( temp );
		temp = randomCache3.nextInt ( songs.size ( ) );
		return temp;
	}
	
	private int generateLoopNext ( int songListPosition , int loopStatus ) {
		switch ( loopStatus ) {
			case 0 :
				songListPosition ++ ;
				break;
			
			case 1 :
				break;
			
			case 2 :
				songListPosition = generateRamdomNumber ( );
				break;
			
			default :
				break;
		}
		return songListPosition;
	}
	
	private boolean playASong ( int songListPosition , int surrSongLoopPosition ) {
		Song song = songs.get ( songListPosition );
		// �Ѹ�������ָ�����ʼ״̬
		mediaPlayer.reset ( );
		mediaPlayer.setAudioStreamType ( AudioManager.STREAM_MUSIC );
		try {
			mediaPlayer.setDataSource ( song.getURL ( ) );
			// ���л���
			mediaPlayer.prepare ( );
		} catch ( Exception e ) {
			e.printStackTrace ( );
		}
		
		int result = audioManager.requestAudioFocus ( myOnAudioFocusChangeListener , AudioManager.STREAM_MUSIC , AudioManager.AUDIOFOCUS_GAIN );
		
		if ( result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ) {
			this.isPlaying = true;
			this.currSongPositionInList = songListPosition;
			initLrc ( );
			mediaPlayer.setOnPreparedListener ( new PreparedListener ( surrSongLoopPosition ) );
			mediaPlayer.setOnCompletionListener ( new CompletionListener ( ) );
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * ��ʼ���������
	 */
	public void initLrc ( ) {
		mLyricProcessor = new LyricProcessor ( );
		// ��ȡ����ļ�
		mLyricProcessor.readLRCFromSDPath ( songs.get ( currSongPositionInList ).getURL ( ) );
		// ���ش����ĸ���ļ�
		lrcList = mLyricProcessor.getLrcList ( );
		PlayerFramgment.playerLyricView.setmLrcList ( lrcList );
		// �л���������ʾ���
		PlayerFramgment.playerLyricView.setAnimation ( AnimationUtils.loadAnimation ( this , R.anim.reloard_lyric ) );
	}
	
	/**
	 * ����ʱ���ȡ�����ʾ������ֵ
	 */
	public int lrcIndex ( ) {
		int duration = ( int ) songs.get ( currSongPositionInList ).getDuration ( );
		if ( currSongLoopProcess < duration ) {
			for ( int i = 0 ; i < lrcList.size ( ) ; i ++ ) {
				if ( i < lrcList.size ( ) - 1 ) {
					if ( currSongLoopProcess < lrcList.get ( i ).getLrcTime ( ) && i == 0 ) {
						index = i;
					}
					if ( currSongLoopProcess > lrcList.get ( i ).getLrcTime ( ) && currSongLoopProcess < lrcList.get ( i + 1 ).getLrcTime ( ) ) {
						index = i;
					}
				}
				if ( i == lrcList.size ( ) - 1 && currSongLoopProcess > lrcList.get ( i ).getLrcTime ( ) ) {
					index = i;
				}
			}
		}
		return index;
	}
	
	private final class CompletionListener implements OnCompletionListener {
		
		@ Override
		public void onCompletion ( MediaPlayer mp ) {
			int songListPosition = generateLoopNext ( currSongPositionInList , loopStatus );
			if ( songListPosition > songs.size ( ) - 1 ) {
				songListPosition = songs.size ( ) - 1;
			}
			if ( ! playASong ( songListPosition , 0 ) ) {
				return;
			}
			// �ı�player��UIΪ��ͣ
			Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
			intentPause.putExtra ( "playBtnTag" , 1 );
			localBroadcastManager2.sendBroadcast ( intentPause );
			// ��player����songid
			Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
			intentPlayerText.putExtra ( "songId" , currSongPositionInList );
			localBroadcastManager2.sendBroadcast ( intentPlayerText );
			// �ı�list��UIΪ��ͣ
			Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
			intentListPause.putExtra ( "listBtnTag" , 1 );
			localBroadcastManager2.sendBroadcast ( intentListPause );
			// ��list����songid
			Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
			intentListPlayerText.putExtra ( "songId" , currSongPositionInList );
			localBroadcastManager2.sendBroadcast ( intentListPlayerText );
		}
		
	}
	
	private final class PreparedListener implements OnPreparedListener {
		private int surrSongLoopPosition;
		
		public PreparedListener ( int surrSongLoopPosition ) {
			this.surrSongLoopPosition = surrSongLoopPosition;
		}
		
		@ Override
		public void onPrepared ( MediaPlayer mp ) {
			mediaPlayer.start ( );
			mediaPlayer.seekTo ( surrSongLoopPosition );
		}
	}
	
	public class PlayerStatuUpdateBroadCastReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			if ( mediaPlayer == null ) {
				return;
			}
			int playerProcess = intent.getIntExtra ( "playerProcess" , 0 );
			int songListPosition = generateLoopNext ( MainPlayerService.this.currSongPositionInList , 1 );
			playASong ( songListPosition , playerProcess * mediaPlayer.getDuration ( ) / 100 );
			// �ı�player��UIΪ��ͣ
			Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
			intentPause.putExtra ( "playBtnTag" , 1 );
			localBroadcastManager2.sendBroadcast ( intentPause );
			// �ı�list��UIΪ��ͣ
			Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
			intentListPause.putExtra ( "listBtnTag" , 1 );
			localBroadcastManager2.sendBroadcast ( intentListPause );
		}
	}
	
	public class UpdateListFragFirstViewBroadCastReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			if ( intent.getIntExtra ( "isFromListFrag" , - 1 ) != 0 ) {
				return;
			}
			// �ı�list��UIΪ����
			Intent intentListPause = new Intent ( MyConstant.ACTION_BROADCAST_LIST_BTN_UPDATE );
			intentListPause.putExtra ( "listBtnTag" , ( ( mediaPlayer == null ) || ( ! mediaPlayer.isPlaying ( ) ) ? 0 : 1 ) );
			localBroadcastManager2.sendBroadcast ( intentListPause );
			// ��list����songid
			Intent intentListPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_LIST_INFO_UPDATE );
			intentListPlayerText.putExtra ( "songId" , currSongPositionInList );
			intentListPlayerText.putExtra ( "isStartFirstTime" , 0 );
			localBroadcastManager2.sendBroadcast ( intentListPlayerText );
		}
	}
	
	public class UpdatePlayerFragRebackBroadCastReceiver extends BroadcastReceiver {
		
		@ Override
		public void onReceive ( Context context , Intent intent ) {
			if ( intent.getIntExtra ( "isFromPlayerFrag" , - 1 ) != 0 ) {
				return;
			}
			// �ı�player��UIΪ���ڲ������
			Intent intentPause = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_BTN_UPDATE );
			intentPause.putExtra ( "playBtnTag" , ( ( mediaPlayer == null ) || ( ! mediaPlayer.isPlaying ( ) ) ? 0 : 1 ) );
			localBroadcastManager2.sendBroadcast ( intentPause );
			// ��player����songid
			Intent intentPlayerText = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_COMPLETE_UPDATE );
			intentPlayerText.putExtra ( "songId" , currSongPositionInList );
			localBroadcastManager2.sendBroadcast ( intentPlayerText );
			// ��loopstatus���͸�player
			Intent intentLoopStatus = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_LOOPLOGO_UPDATE );
			intentLoopStatus.putExtra ( "loopLogo" , loopStatus );
			localBroadcastManager2.sendBroadcast ( intentLoopStatus );
			// ��ʵʱ���Ž��ȷ��͸�player
			Intent intentSeekBar = new Intent ( MyConstant.ACTION_BROADCAST_PLAYER_SEEKBAR_UPDATE );
			int positionProcess = 0;
			int duration = 0;
			positionProcess = currSongLoopProcess;
			duration = ( int ) songs.get ( currSongPositionInList ).getDuration ( );
			intentSeekBar.putExtra ( "positionProcess" , positionProcess );
			intentSeekBar.putExtra ( "duration" , duration );
			localBroadcastManager2.sendBroadcast ( intentSeekBar );
			initLrc ( );
		}
	}
	
	private class MyOnAudioFocusChangeListener implements OnAudioFocusChangeListener {
		@ Override
		public void onAudioFocusChange ( int focusChange ) {
			if ( MainPlayerService.mediaPlayer.isPlaying ( ) ) {
				Intent intent1 = new Intent ( MainPlayerService.this , com.example.service.MainPlayerService.class );
				intent1.putExtra ( "type" , 1 );
				intent1.putExtra ( "playOperation" , 0 );
				startService ( intent1 );
			}
		}
	}
}
