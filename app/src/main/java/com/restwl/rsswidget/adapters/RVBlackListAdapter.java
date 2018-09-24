package com.restwl.rsswidget.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import com.restwl.rsswidget.R;
import com.restwl.rsswidget.model.News;
import com.restwl.rsswidget.utils.HelperUtils;

public class RVBlackListAdapter extends RecyclerView.Adapter<RVBlackListAdapter.BlackListItemViewHolder> {

    private final List<News> newsList;
    private final Resources resources;
    private final LayoutInflater layoutInflater;
    private CellClickListener listener;

    public RVBlackListAdapter(Context context, List<News> newsList) {
        this.newsList = newsList;
        resources = context.getResources();
        layoutInflater = LayoutInflater.from(context);
    }

    public RVBlackListAdapter(Context context, List<News> newsList, CellClickListener listener) {
        this(context, newsList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public BlackListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recycler_view_cell_black_list_item, parent, false);
        return new BlackListItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlackListItemViewHolder holder, int position) {
        News news = newsList.get(position);
        String strWithTab = resources.getString(R.string.placeholder_string_tab_text);
        holder.tvTitle.setText(String.format(strWithTab, news.getTitle()));
        holder.tvDescription.setText(String.format(strWithTab, news.getDescription()));
        holder.tvPubDate.setText(HelperUtils.convertDateToRuLocal(news.getPubDate()));
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    class BlackListItemViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvDescription, tvPubDate;
        final ImageButton button;
        final ViewGroup rootContainer;

        BlackListItemViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvPubDate = itemView.findViewById(R.id.tv_pub_date);
            button = itemView.findViewById(R.id.button_restore);
            rootContainer = itemView.findViewById(R.id.root_container);
            button.setOnClickListener(this::onHideButtonClick);
            rootContainer.setOnClickListener(this::onHideButtonClick);
        }

        private void onHideButtonClick(View view) {
            int position = getAdapterPosition();
            if (listener == null || position == RecyclerView.NO_POSITION) return;
            int viewId = view.getId();
            switch (viewId) {
                case R.id.button_restore:
                    listener.onHideButtonClick(view, newsList.get(position), position);
                    break;
                case R.id.root_container:
                    listener.onItemClick(view, newsList.get(position), position);
                    break;
            }
        }
    }

    public interface CellClickListener {
        void onHideButtonClick(View view, News news, int index);

        void onItemClick(View viewGroup, News news, int index);
    }

}
