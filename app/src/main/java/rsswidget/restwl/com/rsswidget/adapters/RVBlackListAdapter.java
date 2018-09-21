package rsswidget.restwl.com.rsswidget.adapters;

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

import rsswidget.restwl.com.rsswidget.R;
import rsswidget.restwl.com.rsswidget.model.LocalNews;
import rsswidget.restwl.com.rsswidget.utils.HelperUtils;

public class RVBlackListAdapter extends RecyclerView.Adapter<RVBlackListAdapter.HiddenNewsViewHolder> {

    private final List<LocalNews> localNews;
    private final Resources resources;
    private final LayoutInflater layoutInflater;
    private CellClickListener listener;

    public RVBlackListAdapter(Context context, List<LocalNews> localNews) {
        this.localNews = localNews;
        resources = context.getResources();
        layoutInflater = LayoutInflater.from(context);
    }

    public RVBlackListAdapter(Context context, List<LocalNews> localNews, CellClickListener listener) {
        this(context, localNews);
        this.listener = listener;
    }

    @NonNull
    @Override
    public HiddenNewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recycler_view_cell_blocked_news, parent, false);
        return new HiddenNewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HiddenNewsViewHolder holder, int position) {
        LocalNews news = localNews.get(position);

        String strWithTab = resources.getString(R.string.string_with_tab);

        holder.tvTitle.setText(String.format(strWithTab, news.getTitle()));
        holder.tvDescription.setText(String.format(strWithTab, news.getDescription()));
        holder.tvPubDate.setText(HelperUtils.convertDateToRuLocal(news.convertDate()));
    }

    @Override
    public int getItemCount() {
        return localNews.size();
    }

    class HiddenNewsViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvDescription, tvPubDate;
        final ImageButton button;
        final ViewGroup rootContainer;

        HiddenNewsViewHolder(View itemView) {
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
                    listener.onHideButtonClick(view, localNews.get(position), position);
                    break;
                case R.id.root_container:
                    listener.onItemClick(view, localNews.get(position), position);
                    break;
            }
        }
    }

    public interface CellClickListener {
        void onHideButtonClick(View view, LocalNews news, int index);

        void onItemClick(View viewGroup, LocalNews news, int index);
    }

}
