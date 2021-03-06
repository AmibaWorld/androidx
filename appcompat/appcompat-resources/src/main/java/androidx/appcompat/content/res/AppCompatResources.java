/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appcompat.content.res;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ResourceManagerInternal;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ColorStateListInflaterCompat;

import org.xmlpull.v1.XmlPullParser;

import java.util.WeakHashMap;

/**
 * Class for accessing an application's resources through AppCompat, and thus any backward
 * compatible functionality.
 */
@SuppressLint("RestrictedAPI") // Temporary until we have correct restriction scopes for 1.0
public final class AppCompatResources {

    private static final String LOG_TAG = "AppCompatResources";
    private static final ThreadLocal<TypedValue> TL_TYPED_VALUE = new ThreadLocal<>();

    private static final WeakHashMap<Context, SparseArray<ColorStateListCacheEntry>>
            sColorStateCaches = new WeakHashMap<>(0);

    private static final Object sColorStateCacheLock = new Object();

    private AppCompatResources() {}

    /**
     * Returns the {@link ColorStateList} from the given resource. The resource can include
     * themeable attributes, regardless of API level.
     *
     * @param context context to inflate against
     * @param resId the resource identifier of the ColorStateList to retrieve
     */
    public static ColorStateList getColorStateList(@NonNull Context context, @ColorRes int resId) {
        if (Build.VERSION.SDK_INT >= 23) {
            // On M+ we can use the framework
            return context.getColorStateList(resId);
        }

        // Before that, we'll try handle it ourselves
        ColorStateList csl = getCachedColorStateList(context, resId);
        if (csl != null) {
            return csl;
        }
        // Cache miss, so try and inflate it ourselves
        csl = inflateColorStateList(context, resId);
        if (csl != null) {
            // If we inflated it, add it to the cache and return
            addColorStateListToCache(context, resId, csl);
            return csl;
        }

        // If we reach here then we couldn't inflate it, so let the framework handle it
        return ContextCompat.getColorStateList(context, resId);
    }

    /**
     * Return a drawable object associated with a particular resource ID.
     *
     * <p>This method supports inflation of {@code <vector>}, {@code <animated-vector>} and
     * {@code <animated-selector>} resources on devices where platform support is not available.</p>
     *
     * @param context context to inflate against
     * @param resId   The desired resource identifier, as generated by the aapt
     *                tool. This integer encodes the package, type, and resource
     *                entry. The value 0 is an invalid identifier.
     * @return Drawable An object that can be used to draw this resource.
     * @see ContextCompat#getDrawable(Context, int)
     */
    @Nullable
    public static Drawable getDrawable(@NonNull Context context, @DrawableRes int resId) {
        return ResourceManagerInternal.get().getDrawable(context, resId);
    }

    /**
     * Inflates a {@link ColorStateList} from resources, honouring theme attributes.
     */
    @Nullable
    private static ColorStateList inflateColorStateList(Context context, int resId) {
        if (isColorInt(context, resId)) {
            // The resource is a color int, we can't handle it so return null
            return null;
        }

        final Resources r = context.getResources();
        final XmlPullParser xml = r.getXml(resId);
        try {
            return ColorStateListInflaterCompat.createFromXml(r, xml, context.getTheme());
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to inflate ColorStateList, leaving it to the framework", e);
        }
        return null;
    }

    @Nullable
    private static ColorStateList getCachedColorStateList(@NonNull Context context,
            @ColorRes int resId) {
        synchronized (sColorStateCacheLock) {
            final SparseArray<ColorStateListCacheEntry> entries = sColorStateCaches.get(context);
            if (entries != null && entries.size() > 0) {
                final ColorStateListCacheEntry entry = entries.get(resId);
                if (entry != null) {
                    if (entry.configuration.equals(context.getResources().getConfiguration())) {
                        // If the current configuration matches the entry's, we can use it
                        return entry.value;
                    } else {
                        // Otherwise we'll remove the entry
                        entries.remove(resId);
                    }
                }
            }
        }
        return null;
    }

    private static void addColorStateListToCache(@NonNull Context context, @ColorRes int resId,
            @NonNull ColorStateList value) {
        synchronized (sColorStateCacheLock) {
            SparseArray<ColorStateListCacheEntry> entries = sColorStateCaches.get(context);
            if (entries == null) {
                entries = new SparseArray<>();
                sColorStateCaches.put(context, entries);
            }
            entries.append(resId, new ColorStateListCacheEntry(value,
                    context.getResources().getConfiguration()));
        }
    }

    private static boolean isColorInt(@NonNull Context context, @ColorRes int resId) {
        final Resources r = context.getResources();

        final TypedValue value = getTypedValue();
        r.getValue(resId, value, true);

        return value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                && value.type <= TypedValue.TYPE_LAST_COLOR_INT;
    }

    @NonNull
    private static TypedValue getTypedValue() {
        TypedValue tv = TL_TYPED_VALUE.get();
        if (tv == null) {
            tv = new TypedValue();
            TL_TYPED_VALUE.set(tv);
        }
        return tv;
    }

    private static class ColorStateListCacheEntry {
        final ColorStateList value;
        final Configuration configuration;

        ColorStateListCacheEntry(@NonNull ColorStateList value,
                @NonNull Configuration configuration) {
            this.value = value;
            this.configuration = configuration;
        }
    }

}
