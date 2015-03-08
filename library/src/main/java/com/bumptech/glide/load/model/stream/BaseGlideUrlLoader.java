package com.bumptech.glide.load.model.stream;

import android.text.TextUtils;

import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;
import com.bumptech.glide.load.model.ModelCache;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A base class for loading data over http/https. Can be subclassed for use with any model that can
 * be translated in to {@link java.io.InputStream} data.
 *
 * @param <Model> The type of the model.
 */
public abstract class BaseGlideUrlLoader<Model> implements ModelLoader<Model, InputStream> {
  private final ModelLoader<GlideUrl, InputStream> concreteLoader;
  private final ModelCache<Model, GlideUrl> modelCache;

  protected BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader) {
    this(concreteLoader, null);
  }

  protected BaseGlideUrlLoader(ModelLoader<GlideUrl, InputStream> concreteLoader,
      ModelCache<Model, GlideUrl> modelCache) {
    this.concreteLoader = concreteLoader;
    this.modelCache = modelCache;
  }

  @Override
  public LoadData<InputStream> buildLoadData(Model model, int width, int height,
      Map<String, Object> options) {
    GlideUrl result = null;
    if (modelCache != null) {
      result = modelCache.get(model, width, height);
    }

    if (result == null) {
      String stringURL = getUrl(model, width, height);
      if (TextUtils.isEmpty(stringURL)) {
        return null;
      }

      result = new GlideUrl(stringURL, getHeaders(model, width, height));

      if (modelCache != null) {
        modelCache.put(model, width, height, result);
      }
    }

    // TODO: this is expensive and slow to calculate every time, we should either cache these, or
    // try to come up with a way to avoid finding them when not necessary.
    List<String> alternateUrls = getAlternateUrls(model, width, height);
    LoadData<InputStream> concreteLoaderData = concreteLoader.buildLoadData(result, width, height,
        options);
    if (alternateUrls.isEmpty()) {
      return concreteLoaderData;
    } else {
      return new LoadData<>(concreteLoaderData.sourceKey, getAlternateKeys(alternateUrls),
          concreteLoaderData.fetcher);
    }
  }

  private static List<Key> getAlternateKeys(List<String> alternateUrls) {
    List<Key> result = new ArrayList<>(alternateUrls.size());
    for (String alternate : alternateUrls) {
      result.add(new ObjectKey(alternate));
    }
    return result;
  }

  /**
   * Returns a valid url http:// or https:// for the given model and dimensions as a string.
   *
   * @param model  The model.
   * @param width  The width in pixels of the view/target the image will be loaded into.
   * @param height The height in pixels of the view/target the image will be loaded into.
   */
  protected abstract String getUrl(Model model, int width, int height);

  /**
   * Returns a list of alternate urls for the given model, width, and height from which equivalent
   * data can be obtained (usually the same image with the same aspect ratio, but in a larger size)
   * as the primary url.
   *
   * <p> Implementing this method allows Glide to fulfill requests for bucketed images in smaller
   * bucket sizes using already cached data for larger bucket sizes. </p>
   *
   * @param width  The width in pixels of the view/target the image will be loaded into.
   * @param height The height in pixels of the view/target the image will be loaded into.
   */
  protected List<String> getAlternateUrls(Model model, int width, int height) {
    return Collections.emptyList();
  }

  /**
   * Returns the headers for the given model and dimensions as a map of strings to sets of strings,
   * or null if no headers should be added.
   *
   * @param model The model.
   * @param width The width in pixels of the view/target the image will be loaded into.
   * @param height The height in pixels of the view/target the image will be loaded into.
   */
  protected Headers getHeaders(Model model, int width, int height) {
    return Headers.NONE;
  }
}
