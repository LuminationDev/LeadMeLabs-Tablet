package com.lumination.leadmelabs.models.applications.information;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayout;
import com.lumination.leadmelabs.R;
import com.lumination.leadmelabs.models.applications.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Utility class containing methods for handling tags in Android applications.
 */
public class TagUtils {
    private static final HashMap<String, Integer> DEFAULT_TAG_DRAWABLE_MAP = new HashMap<>();

    // Initialize default tag drawable map
    static {
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HASS, R.drawable.tag_yellow_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.MATHS, R.drawable.tag_blue_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.SCIENCE, R.drawable.tag_green_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ARTS, R.drawable.tag_purple_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.HEALTH_PE, R.drawable.tag_orange_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.ENGLISH, R.drawable.tag_rose_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.DESIGN_TECH, R.drawable.tag_teal_curved);
        DEFAULT_TAG_DRAWABLE_MAP.put(TagConstants.LANGUAGES, R.drawable.tag_fuchsia_curved);
    }

    /**
     * Sets up the tags and sub-tags for a given application.
     * This method populates a LinearLayout container with main tags extracted from the provided application.
     * It also sets up a TextView to display sub-tags for the application, if available.
     * Main tags and sub-tags are extracted from the Application object and displayed accordingly.
     *
     * @param context               The context of the application.
     * @param tagsContainer         The LinearLayout container to which main tags will be added.
     * @param subTagsTextView       The TextView to display sub-tags.
     * @param currentApplication    The Application object containing tag information.
     */
    public static void setupTags(Context context, LinearLayout tagsContainer, TextView subTagsTextView, FlexboxLayout complexityView, Application currentApplication) {

        // Setup main tags
        setupMainTags(context, tagsContainer, currentApplication.getInformation().getTags());

        // Setup sub-tags
        setupSubTags(subTagsTextView, currentApplication.getInformation().getSubTags());

        setupComplexity(complexityView, currentApplication.getInformation().getComplexity());
    }

    /**
     * Adds tags to a LinearLayout container dynamically.
     *
     * @param context         The context of the application.
     * @param tagsContainer  The LinearLayout container to which tags will be added.
     * @param tags            The list of tags to be added.
     */
    private static void setupMainTags(Context context, LinearLayout tagsContainer, List<String> tags) {
        // Clear existing tags if any
        tagsContainer.removeAllViews();

        // Add tags dynamically
        if (tags.isEmpty()) {
            tags.add(TagConstants.DEFAULT);
        }

        // If there are more than 3 tags, just show the first then (+X more)
        if (tags.size() > 2) {
            createTag(context, tagsContainer, tags.get(0));

            String extraTags = String.format(Locale.ENGLISH, "+ %d more", tags.size() - 1);
            createTag(context, tagsContainer, extraTags);
        } else {
            for (String tag : tags) {
                createTag(context, tagsContainer, tag);
            }
        }
    }

    private static void createTag(Context context, LinearLayout tagsContainer, String tag) {
        // Inflate the tag layout
        View tagLayout = LayoutInflater.from(context).inflate(R.layout.card_tag, tagsContainer, false);

        // Get the TextView from the inflated layout
        TextView tagTextView = tagLayout.findViewById(R.id.tagText);

        // Set the tag text
        tagTextView.setText(tag);

        // Set the background resource for the tag
        Integer backgroundResource = DEFAULT_TAG_DRAWABLE_MAP.get(tag);
        if (backgroundResource != null) {
            tagLayout.setBackgroundResource(backgroundResource);
        } else {
            // Set a default background if no specific background is available
            tagLayout.setBackgroundResource(R.drawable.tag_default_curved);
        }

        // Add the tag layout to the tags container
        tagsContainer.addView(tagLayout);
    }

    /**
     *
     * @param subTagsTextView
     * @param subTags
     */
    private static void setupSubTags(TextView subTagsTextView, List<String> subTags) {
        String subTagsText = TextUtils.join(", ", subTags);
        if (TextUtils.isEmpty(subTagsText)) {
            subTagsTextView.setVisibility(View.GONE);
        } else {
            subTagsTextView.setVisibility(View.VISIBLE);
            subTagsTextView.setText(subTagsText);
        }
    }

    private static void setupComplexity(FlexboxLayout complexityView, String complexity) {
        if (complexity == null || complexity.isEmpty()) {
            complexityView.setVisibility(View.GONE);
            return;
        }
        TextView complexityText = complexityView.findViewById(R.id.complexity_level);
        ImageView complexityIcon = complexityView.findViewById(R.id.complexity_icon);
        complexityText.setText(complexity);
        switch (complexity) {
            case TagConstants.SIMPLE:
                complexityIcon.setImageResource(R.drawable.grey_difficulty_simple);
                break;
            case TagConstants.INTERMEDIATE:
                complexityIcon.setImageResource(R.drawable.grey_difficulty_intermediate);
                break;
            case TagConstants.COMPLEX:
                complexityIcon.setImageResource(R.drawable.grey_difficulty_advanced);
                break;
        }
    }
}
