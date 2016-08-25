package com.example.song;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.example.activity.R;
import com.example.fragment.SongListFramgment;
import com.example.quickaccessbar.QuickLetterModel;
import com.example.utils.MyConstant;

public class SongListAdapter extends BaseAdapter implements SectionIndexer {
	Context context;
	List < Song > songs;
	SongListFramgment songListFrag;
	private List < QuickLetterModel > quickLetterModelList;
	
	public SongListAdapter ( List < Song > songs , Context context , SongListFramgment songListFrag ,List < QuickLetterModel > quickLetterModelList) {
		this.context = context;
		this.songs = songs;
		this.songListFrag = songListFrag;
		this.quickLetterModelList = quickLetterModelList;
	}
	
	/**
	 * ��ListView���ݷ����仯ʱ,���ô˷���������ListView
	 * 
	 * @param list
	 */
	public void updateListView ( List < QuickLetterModel > quickLetterModelList ) {
		this.quickLetterModelList = quickLetterModelList;
		notifyDataSetChanged ( );
	}
	
	@ Override
	public int getCount ( ) {
		return songs.size ( );
	}
	
	@ Override
	public Object getItem ( int position ) {
		return position;
	}
	
	@ Override
	public long getItemId ( int position ) {
		return position;
	}
	
	@ Override
	public View getView ( int position , View convertView , ViewGroup parent ) {
		//TODO ��viewHolder�����Ż���
		boolean isItemSelected = ( songListFrag.memoryListTempView [ 1 ] == position );
		
		View layout = LayoutInflater.from ( context ).inflate ( R.layout.song_list_item , null );
		
		TextView songTitleTextView = ( TextView ) layout.findViewById ( R.id.list_song_title );
		songTitleTextView.setText ( ( String ) songs.get ( position ).getTitle ( ) );
		
		String author = ( String ) songs.get ( position ).getAuthor ( );
		String album = ( String ) songs.get ( position ).getSongAlbum ( );
		TextView songInfoTextView = ( TextView ) layout.findViewById ( R.id.list_song_info );
		songInfoTextView.setText ( author + MyConstant.CONNECTION_WORD + album );
		
		TextView songDurationTextView = ( TextView ) layout.findViewById ( R.id.list_song_duration );
		songDurationTextView.setText ( setDuration2String ( ( int ) songs.get ( position ).getDuration ( ) ) );
		
		TextView songNumberTextView = ( TextView ) layout.findViewById ( R.id.list_song_number );
		ImageView songIconImageView = ( ImageView ) layout.findViewById ( R.id.list_song_icon );
		
		if ( isItemSelected ) {
			songNumberTextView.setVisibility ( View.GONE );
			songIconImageView.setVisibility ( View.VISIBLE );
			songTitleTextView.setTextColor ( context.getResources ( ).getColor ( R.color.text_dark ) );
			songInfoTextView.setTextColor ( context.getResources ( ).getColor ( R.color.text_dark ) );
			songDurationTextView.setTextColor ( context.getResources ( ).getColor ( R.color.text_dark ) );
		} else {
			songNumberTextView.setVisibility ( View.VISIBLE );
			songIconImageView.setVisibility ( View.GONE );
			songNumberTextView.setText ( String.format ( "%03d" , position + 1 ) );
		}
		
		return layout;
	}
	
	private String setDuration2String ( int duration ) {
		duration /= 1000;
		int second = duration % 60;
		int minute = duration / 60;
		return String.format ( "%02d:%02d" , minute , second );
	}
	
	/**
	 * ����ListView�ĵ�ǰλ�û�ȡ���������ĸ��Char asciiֵ
	 */
	public int getSectionForPosition ( int position ) {
		return quickLetterModelList.get ( position ).getQuickLetters ( ).charAt ( 0 );
	}
	
	/**
	 * ���ݷ��������ĸ��Char asciiֵ��ȡ���һ�γ��ָ�����ĸ��λ��
	 */
	public int getPositionForSection ( int section ) {
		for ( int i = 0 ; i < getCount ( ) ; i ++ ) {
			String sortStr = quickLetterModelList.get ( i ).getQuickLetters ( );
			char firstChar = sortStr.toUpperCase ( ).charAt ( 0 );
			if ( firstChar == section ) {
				return i;
			}
		}
		
		return - 1;
	}
	
	@ Override
	public Object [ ] getSections ( ) {
		return null;
	}
	
}
