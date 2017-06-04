package io.github.froger.instamaterial.ui.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.froger.instamaterial.InstaMaterialApplication;
import io.github.froger.instamaterial.R;
import io.github.froger.instamaterial.ui.activity.MainActivity;
import io.github.froger.instamaterial.ui.view.LoadingFeedItemView;

public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String ACTION_LIKE_BUTTON_CLICKED = "action_like_button_button";
    static final String ACTION_LIKE_IMAGE_CLICKED = "action_like_image_button";

    static final int VIEW_TYPE_DEFAULT = 1;
    private static final int VIEW_TYPE_LOADER = 2;

    private final List<FeedItem> feedItems = new ArrayList<>();

    private String username;
    private Context context;
    private SQLiteDatabase chatDBlocal;
    private OnFeedItemClickListener onFeedItemClickListener;

    private boolean showLoadingView = false;

    public FeedAdapter(Context context, SQLiteDatabase db) {
        this.context = context;
        this.chatDBlocal = db;
        this.username = "not_login";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void loadLikes() {
        Cursor likedPhotos = chatDBlocal.rawQuery("SELECT DISTINCT post_id FROM likes WHERE user = '"
                + username + "' AND value = 1", null);
        if (likedPhotos == null)
            return;
        likedPhotos.moveToFirst();
        for (int j = 0; j < likedPhotos.getCount(); j++) {
            for (int i = 0; i < feedItems.size(); i++)
                if (feedItems.get(i).id == likedPhotos.getInt(0)) {
                    feedItems.get(i).isLiked = true;
                    notifyItemChanged(i, new Object());
                    break;
                }
            likedPhotos.moveToNext();
        }
        likedPhotos.close();

        Cursor dislikedPhotos = chatDBlocal.rawQuery("SELECT DISTINCT post_id FROM likes WHERE user = '"
                + username + "' AND value = -1", null);
        if (dislikedPhotos == null)
            return;
        dislikedPhotos.moveToFirst();
        for (int j = 0; j < dislikedPhotos.getCount(); j++) {
            for (int i = 0; i < feedItems.size(); i++)
                if (feedItems.get(i).id == dislikedPhotos.getInt(0)) {
                    feedItems.get(i).isDisliked = true;
                    notifyItemChanged(i, new Object());
                    break;
                }
            dislikedPhotos.moveToNext();
        }
        dislikedPhotos.close();
    }

    private class LikeAddition extends AsyncTask<Void, Void, Void> {
        String username;
        int postId;

        LikeAddition(String username, int postId) {
            this.username = username;
            this.postId = postId;
        }

        LikeAddition(int postId) {
            if (context instanceof MainActivity) {
                InstaMaterialApplication myApp = (InstaMaterialApplication) ((MainActivity) context).getApplication();
                this.username = myApp.getUser();
            }
            this.postId = postId;
        }

        protected Void doInBackground(Void... urls) {
            try {
                URL url = new URL("http://u0306965.plsk.regruhosting.ru/" +
                        "chat.php?action=like" +
                        "&post_id=" + postId +
                        "&user=" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // ждем 10сек
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();
                connection.getResponseCode();

                ContentValues new_mess = new ContentValues();
                new_mess.put("user", username);
                new_mess.put("post_id", postId);
                new_mess.put("value", 1);
                chatDBlocal.insert("likes", null, new_mess);
                new_mess.clear();
                chatDBlocal.execSQL("UPDATE chat SET count = count + '1' WHERE _id = '" + postId +"'");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

    }

    private class LikeDeletion extends AsyncTask<Void, Void, Void> {
        String username;
        int postId;

        LikeDeletion(String username, int postId) {
            this.username = username;
            this.postId = postId;
        }

        LikeDeletion(int postId) {
            if (context instanceof MainActivity) {
                InstaMaterialApplication myApp = (InstaMaterialApplication) ((MainActivity) context).getApplication();
                this.username = myApp.getUser();
            }
            this.postId = postId;
        }

        protected Void doInBackground(Void... urls) {
            try {
                URL url = new URL("http://u0306965.plsk.regruhosting.ru/" +
                        "chat.php?action=delete_like" +
                        "&post_id=" + postId +
                        "&user=" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // ждем 10сек
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();
                connection.getResponseCode();
                chatDBlocal.delete("likes", "post_id = " + postId + " AND user = '" + username + "'", null);
                chatDBlocal.execSQL("UPDATE chat SET count = count - '1' WHERE _id = '" + postId +"'");
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private class DislikeAddition extends AsyncTask<Void, Void, Void> {
        String username;
        int postId;

        DislikeAddition(String username, int postId) {
            this.username = username;
            this.postId = postId;
        }

        DislikeAddition(int postId) {
            if (context instanceof MainActivity) {
                InstaMaterialApplication myApp = (InstaMaterialApplication) ((MainActivity) context).getApplication();
                this.username = myApp.getUser();
            }
            this.postId = postId;
        }

        protected Void doInBackground(Void... urls) {
            try {
                URL url = new URL("http://u0306965.plsk.regruhosting.ru/" +
                        "chat.php?action=dislike" +
                        "&post_id=" + postId +
                        "&user=" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // ждем 10сек
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();
                connection.getResponseCode();

                ContentValues new_mess = new ContentValues();
                new_mess.put("user", username);
                new_mess.put("post_id", postId);
                new_mess.put("value", -1);
                chatDBlocal.insert("likes", null, new_mess);
                new_mess.clear();
                chatDBlocal.execSQL("UPDATE chat SET count = count - '1' WHERE _id = '" + postId +"'");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class DislikeDeletion extends AsyncTask<Void, Void, Void> {
        String username;
        int postId;

        DislikeDeletion(String username, int postId) {
            this.username = username;
            this.postId = postId;
        }

        DislikeDeletion(int postId) {
            if (context instanceof MainActivity) {
                InstaMaterialApplication myApp = (InstaMaterialApplication) ((MainActivity) context).getApplication();
                this.username = myApp.getUser();
            }
            this.postId = postId;
        }

        protected Void doInBackground(Void... urls) {
            try {
                URL url = new URL("http://u0306965.plsk.regruhosting.ru/" +
                        "chat.php?action=delete_dislike" +
                        "&post_id=" + postId +
                        "&user=" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000); // ждем 10сек
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.connect();
                connection.getResponseCode();

                chatDBlocal.delete("likes", "post_id = " + postId + " AND user = '" + username + "'", null);
                chatDBlocal.execSQL("UPDATE chat SET count = count + '1' WHERE _id = '" + postId +"'");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_feed, parent, false);
            CellFeedViewHolder cellFeedViewHolder = new CellFeedViewHolder(view);
            setupClickableViews(view, cellFeedViewHolder);
            return cellFeedViewHolder;
        } else if (viewType == VIEW_TYPE_LOADER) {
            LoadingFeedItemView view = new LoadingFeedItemView(context);
            view.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            );
            return new LoadingCellFeedViewHolder(view);
        }
        return null;
    }

    private void like(CellFeedViewHolder holder, String action) {
        int adapterPosition = holder.getAdapterPosition();
        FeedItem item = feedItems.get(adapterPosition);
        String username = ((InstaMaterialApplication)((MainActivity)this.context).getApplication())
                .getUser();
        if (item.isDisliked)
            return;
        if (!username.equals("not_login")) {
            if (item.isLiked) {
                item.isLiked = false;
                new LikeDeletion(username, item.id).execute();
                item.likesCount--;
            } else {
                item.isLiked = true;
                new LikeAddition(username, item.id).execute();
                item.likesCount++;
            }
            notifyItemChanged(adapterPosition, action);
        }
        if (context instanceof MainActivity) {
            ((MainActivity) context).showLikedSnackbar();
        }
    }

    private void dislike(CellFeedViewHolder holder, String action) {
        int adapterPosition = holder.getAdapterPosition();
        FeedItem item = feedItems.get(adapterPosition);
        String username = ((InstaMaterialApplication)((MainActivity)this.context).getApplication())
                .getUser();
        if (item.isLiked)
            return;
        if (!username.equals("not_login")) {
            if (item.isDisliked) {
                item.isDisliked = false;
                new DislikeDeletion(username, item.id).execute();
                item.likesCount++;
            } else {
                item.isDisliked = true;
                new DislikeAddition(username, item.id).execute();
                item.likesCount--;
            }
            notifyItemChanged(adapterPosition, new Object());
        }
        if (context instanceof MainActivity) {
            ((MainActivity) context).showLikedSnackbar();
        }
    }

    private void setupClickableViews(final View view, final CellFeedViewHolder cellFeedViewHolder) {
        cellFeedViewHolder.btnComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = ((InstaMaterialApplication)((MainActivity)context).getApplication())
                        .getUser();
                if (username.equals("not_login")) {
                    ((MainActivity) context).showLikedSnackbar();
                    return;
                }
                onFeedItemClickListener.onCommentsClick(view, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = ((InstaMaterialApplication)((MainActivity)context).getApplication())
                        .getUser();
                if (username.equals("not_login")) {
                    ((MainActivity)context).showLikedSnackbar();
                    return;
                }
                onFeedItemClickListener.onMoreClick(v, cellFeedViewHolder.getAdapterPosition());
            }
        });
        cellFeedViewHolder.ivFeedCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            like(cellFeedViewHolder, ACTION_LIKE_IMAGE_CLICKED);
            }
        });
        cellFeedViewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            like(cellFeedViewHolder, ACTION_LIKE_BUTTON_CLICKED);
            }
        });
        cellFeedViewHolder.btnDislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dislike(cellFeedViewHolder, ACTION_LIKE_BUTTON_CLICKED);
            }
        });
        cellFeedViewHolder.ivUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//            onFeedItemClickListener.onProfileClick(view);
            }
        });
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        ((CellFeedViewHolder) viewHolder).bindView(feedItems.get(position));

        if (getItemViewType(position) == VIEW_TYPE_LOADER) {
            bindLoadingFeedItem((LoadingCellFeedViewHolder) viewHolder);
        }
    }

    private void bindLoadingFeedItem(final LoadingCellFeedViewHolder holder) {
        holder.loadingFeedItemView.setOnLoadingFinishedListener(new LoadingFeedItemView.OnLoadingFinishedListener() {
            @Override
            public void onLoadingFinished() {
                showLoadingView = false;
                notifyItemChanged(0);
            }
        });
        holder.loadingFeedItemView.startLoading();
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadingView && position == 0) {
            return VIEW_TYPE_LOADER;
        } else {
            return VIEW_TYPE_DEFAULT;
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    Long last_time = 0L;

    public void updateItems(boolean animated) {
        Cursor cursor;

        cursor = chatDBlocal.rawQuery(
                "SELECT * FROM chat ORDER BY data", null);

        if (cursor.moveToFirst()) {
            do {
                cursor = chatDBlocal.rawQuery(
                        "SELECT * FROM chat WHERE data >" + last_time.toString() + " ORDER BY data", null);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!cursor.moveToFirst());

        }

        cursor.moveToLast();
        last_time = cursor.getLong(cursor
                .getColumnIndex("data"));

        HashMap<String, Object> hm;

        cursor.moveToFirst();
        do {
            hm = new HashMap<>();
            hm.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
            hm.put("author", cursor.getString(cursor.getColumnIndex("author")));
            hm.put("img", cursor.getString(cursor.getColumnIndex("img")));
            hm.put("text", cursor.getString(cursor.getColumnIndex("text")));
            hm.put("likes_count", cursor.getInt(cursor.getColumnIndex("count")));
            hm.put("list_author_time", new SimpleDateFormat(
                    "HH:mm - dd.MM.yyyy").format(new Date(cursor
                    .getLong(cursor.getColumnIndex("data")))));
            feedItems.add(0, new FeedItem(Integer.valueOf(hm.get("likes_count").toString()), false,
                    false, Integer.valueOf(hm.get("id").toString()), hm.get("img").toString(),
                    hm.get("author").toString(), hm.get("text").toString()));
        } while (cursor.moveToNext());
        if (animated) {
            notifyItemRangeInserted(0, feedItems.size());
        } else {
            notifyDataSetChanged();
        }
        cursor.close();
    }

    public void checkNewItems(boolean animated) {
        feedItems.clear();

        Cursor cursor = chatDBlocal.rawQuery(
                "SELECT * FROM chat ORDER BY data", null);
        if (cursor.moveToFirst()) {
            HashMap<String, Object> hm;
            do {
                hm = new HashMap<>();
                hm.put("id", cursor.getInt(cursor.getColumnIndex("_id")));
                hm.put("author", cursor.getString(cursor.getColumnIndex("author")));
                hm.put("img", cursor.getString(cursor.getColumnIndex("img")));
                hm.put("text", cursor.getString(cursor.getColumnIndex("text")));
                hm.put("likes_count", cursor.getInt(cursor.getColumnIndex("count")));
                hm.put("list_author_time", new SimpleDateFormat(
                        "HH:mm - dd.MM.yyyy").format(new Date(cursor
                        .getLong(cursor.getColumnIndex("data")))));
                feedItems.add(0, new FeedItem(Integer.valueOf(hm.get("likes_count").toString()),
                        false, false, Integer.valueOf(hm.get("id").toString()), hm.get("img").toString(),
                        hm.get("author").toString(), hm.get("text").toString()));
            } while (cursor.moveToNext());
        }
        if (animated) {
            notifyItemRangeInserted(0, feedItems.size());
        } else {
            notifyDataSetChanged();
        }
        loadLikes();
    }

    public void setOnFeedItemClickListener(OnFeedItemClickListener onFeedItemClickListener) {
        this.onFeedItemClickListener = onFeedItemClickListener;
    }

    public void showLoadingView() {
        showLoadingView = true;
        notifyItemChanged(0);
    }

    static class CellFeedViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ivFeedCenter)
        ImageView ivFeedCenter;
        @BindView(R.id.tvFeedBottom)
        TextView tvFeedBottom;
        @BindView(R.id.btnComments)
        ImageButton btnComments;
        @BindView(R.id.btnLike)
        ImageButton btnLike;
        @BindView(R.id.btnDislike)
        ImageButton btnDislike;
        @BindView(R.id.btnMore)
        ImageButton btnMore;
        @BindView(R.id.vBgLike)
        View vBgLike;
        @BindView(R.id.ivLike)
        ImageView ivLike;
        @BindView(R.id.tsLikesCounter)
        TextSwitcher tsLikesCounter;
        @BindView(R.id.ivUserProfile)
        ImageView ivUserProfile;
        @BindView(R.id.tvAuthor)
        TextView tvAuthor;
        @BindView(R.id.vImageRoot)
        FrameLayout vImageRoot;

        FeedItem feedItem;

        CellFeedViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
            ImageView bmImage;

            public DownloadImageTask(ImageView bmImage) {
                this.bmImage = bmImage;
            }

            protected Bitmap doInBackground(String... urls) {
                String urldisplay = urls[0];
                Bitmap mIcon11 = null;
                try {
                    InputStream in = new java.net.URL(urldisplay).openStream();
                    mIcon11 = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
                return mIcon11;
            }

            protected void onPostExecute(Bitmap result) {
                bmImage.setImageBitmap(result);
            }
        }

        public void bindView(FeedItem feedItem) {
            this.feedItem = feedItem;
            new DownloadImageTask(ivFeedCenter).execute("http://u0306965.plsk.regruhosting.ru" + feedItem.path);
            tvAuthor.setText(feedItem.author);
            tvFeedBottom.setText(feedItem.text);
            btnLike.setImageResource(feedItem.isLiked ? R.drawable.ic_heart_red : R.drawable.ic_heart_outline_grey);
            btnDislike.setImageResource(feedItem.isDisliked ? R.drawable.ic_minus_red : R.drawable.ic_minus_outline_grey);
            tsLikesCounter.setCurrentText(vImageRoot.getResources().getQuantityString(
                    R.plurals.likes_count, feedItem.likesCount, feedItem.likesCount
            ));
        }

        FeedItem getFeedItem() {
            return feedItem;
        }
    }

    private static class LoadingCellFeedViewHolder extends CellFeedViewHolder {

        LoadingFeedItemView loadingFeedItemView;

        LoadingCellFeedViewHolder(LoadingFeedItemView view) {
            super(view);
            this.loadingFeedItemView = view;
        }

        @Override
        public void bindView(FeedItem feedItem) {
            super.bindView(feedItem);
        }
    }

    static class FeedItem {
        int likesCount;
        boolean isLiked;
        boolean isDisliked;
        public int id;
        String path;
        String author;
        private String text;


        FeedItem(int likesCount, boolean isLiked, boolean isDisliked, int id, String path, String author, String text) {
            this.likesCount = likesCount;
            this.isLiked = isLiked;
            this.path = path;
            this.author = author;
            this.text = text;
            this.id = id;
        }
    }

    public interface OnFeedItemClickListener {
        void onCommentsClick(View v, int position);

        void onMoreClick(View v, int position);

        void onProfileClick(View v);
    }
}
