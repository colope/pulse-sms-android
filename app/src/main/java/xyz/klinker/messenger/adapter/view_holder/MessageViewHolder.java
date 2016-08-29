/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.adapter.view_holder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.ImageViewerActivity;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.receiver.MessageListUpdatedReceiver;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * View holder for working with a message.
 */
public class MessageViewHolder extends RecyclerView.ViewHolder {

    private MessageListFragment fragment;

    public TextView message;
    public TextView timestamp;
    public TextView contact;
    public ImageView image;
    public View messageHolder;
    public long messageId;

    private View.OnLongClickListener messageOptions = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
            String[] items;
            if (message.getVisibility() == View.VISIBLE) {
                items = new String[3];
                items[0] = view.getContext().getString(R.string.view_details);
                items[1] = view.getContext().getString(R.string.delete);
                items[2] = view.getContext().getString(R.string.copy_message);
            } else {
                items = new String[2];
                items[0] = view.getContext().getString(R.string.view_details);
                items[1] = view.getContext().getString(R.string.delete);
            }

            if (fragment == null || !fragment.isDragging()) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                if (which == 0) {
                                    showMessageDetails();
                                } else if (which == 1) {
                                    deleteMessage();
                                } else if (which == 2) {
                                    copyMessageText();
                                }
                            }
                        })
                        .show();

                if (fragment != null) {
                    fragment.setDetailsChoiceDialog(dialog);
                }
            }

            return true;
        }
    };

    public MessageViewHolder(MessageListFragment fragment, final View itemView, int color, final long conversationId) {
        super(itemView);

        this.fragment = fragment;

        message = (TextView) itemView.findViewById(R.id.message);
        timestamp = (TextView) itemView.findViewById(R.id.timestamp);
        contact = (TextView) itemView.findViewById(R.id.contact);
        image = (ImageView) itemView.findViewById(R.id.image);
        messageHolder = itemView.findViewById(R.id.message_holder);

        if (color != -1 && messageHolder != null) {
            messageHolder.setBackgroundTintList(ColorStateList.valueOf(color));
        }

        if (image != null) {
            image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(itemView.getContext(), ImageViewerActivity.class);
                    intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, conversationId);
                    intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageId);
                    itemView.getContext().startActivity(intent);
                }
            });

            image.setOnLongClickListener(messageOptions);
        }

        if (message != null) {
            message.setOnLongClickListener(messageOptions);
        }
    }

    private void showMessageDetails() {
        DataSource source = DataSource.getInstance(message.getContext());
        source.open();

        new AlertDialog.Builder(message.getContext())
                .setMessage(source.getMessageDetails(message.getContext(), messageId))
                .setPositiveButton(android.R.string.ok, null)
                .show();

        source.close();
    }

    private void deleteMessage() {
        DataSource source = DataSource.getInstance(message.getContext());
        source.open();
        Message m = source.getMessage(messageId);
        
        if (m != null) {
            long conversationId = m.conversationId;
            source.deleteMessage(messageId);
            MessageListUpdatedReceiver.sendBroadcast(message.getContext(), conversationId);
        }

        source.close();
    }

    private void copyMessageText() {
        ClipboardManager clipboard = (ClipboardManager)
                message.getContext().getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("messenger",
                message.getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(message.getContext(), R.string.message_copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }

}
