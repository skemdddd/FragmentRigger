package com.jkb.fragment.rigger.rigger;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import com.jkb.fragment.rigger.annotation.Puppet;
import com.jkb.fragment.rigger.exception.NotExistException;
import com.jkb.fragment.rigger.exception.RiggerException;
import com.jkb.fragment.rigger.helper.FragmentExecutor;
import com.jkb.fragment.rigger.helper.FragmentExecutor.Builder;
import com.jkb.fragment.rigger.helper.FragmentStackManager;
import com.jkb.fragment.rigger.utils.Logger;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Fragment Rigger.rig the Fragment puppet.
 *
 * @author JingYeoh
 *         <a href="mailto:yangjing9611@foxmail.com">Email me</a>
 *         <a href="https://github.com/justkiddingbaby">Github</a>
 *         <a href="http://blog.justkiddingbaby.com">Blog</a>
 * @since Nov 20,2017
 */

final class _FragmentRigger extends _Rigger {

  private static final String BUNDLE_KEY_FRAGMENT_TAG = "/bundle/key/fragment/tag";
  private static final String BUNDLE_KEY_FRAGMENT_STATUS_HIDE = "/bundle/key/fragment/status/hide";

  private Fragment mFragment;
  private Activity mActivity;
  private Context mContext;
  private FragmentManager mParentFm;
  private FragmentManager mChildFm;
  //data
  @IdRes
  private int mContainerViewId;
  private String mFragmentTag;
  private FragmentStackManager mStackManager;
  private LinkedList<Builder> mFragmentTransactions;

  _FragmentRigger(@NonNull Fragment fragment) {
    this.mFragment = fragment;
    //init containerViewId
    Class<? extends Fragment> clazz = fragment.getClass();
    Puppet puppet = clazz.getAnnotation(Puppet.class);
    mContainerViewId = puppet.containerViewId();
    //init fragment tag
    mFragmentTag = UUID.randomUUID().toString();
    //init fragment helper
    mFragmentTransactions = new LinkedList<>();
    mStackManager = new FragmentStackManager();
  }

  @Override
  public void onAttach(Context context) {
    mActivity = (Activity) context;
    mContext = context;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    mParentFm = mFragment.getFragmentManager();
    mChildFm = mFragment.getChildFragmentManager();
    if (savedInstanceState != null) {
      mFragmentTag = savedInstanceState.getString(BUNDLE_KEY_FRAGMENT_TAG);
      mStackManager = FragmentStackManager.restoreStack(savedInstanceState);
      restoreHiddenState(savedInstanceState);
    }
  }

  /**
   * Restore the state of fragment hidden.
   */
  private void restoreHiddenState(Bundle savedInstanceState) {
    boolean isHidden = savedInstanceState.getBoolean(BUNDLE_KEY_FRAGMENT_STATUS_HIDE);
    if (isHidden) {
      commitFragmentTransaction(FragmentExecutor.beginTransaction(mParentFm).hide(mFragment));
    } else {
      commitFragmentTransaction(FragmentExecutor.beginTransaction(mParentFm).show(mFragment));
    }
  }

  @Override
  public void onResumeFragments() {
    /*This method is called in Activity,here will never be called*/
  }

  @Override
  public void onResume() {
    //commit all saved fragment transaction.
    while (true) {
      Builder transaction = mFragmentTransactions.poll();
      if (transaction == null) break;
      commitFragmentTransaction(transaction);
    }
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putString(BUNDLE_KEY_FRAGMENT_TAG, mFragmentTag);
    outState.putBoolean(BUNDLE_KEY_FRAGMENT_STATUS_HIDE, mFragment.isHidden());
    mStackManager.saveInstanceState(outState);
  }

  @Override
  public void onDestroy() {
    Logger.d(mFragment, "isAdded=" + mFragment.isAdded());
    Logger.d(mFragment, "isDetached=" + mFragment.isDetached());
    Logger.d(mFragment, "isRemoving=" + mFragment.isRemoving());
    Logger.d(mFragment, "isInLayout=" + mFragment.isInLayout());
  }

  @Override
  public void startFragment(@NonNull Fragment fragment) {

  }

  @Override
  public boolean isResumed() {
    return mFragment.isResumed();
  }

  @Override
  public void close() {
    mStackManager.clear();
  }

  @Override
  public void close(@NonNull Fragment fragment) {
    String fragmentTAG = Rigger.getRigger(fragment).getFragmentTAG();
    if (!mStackManager.remove(fragmentTAG)) {
      throwException(new NotExistException(fragmentTAG));
    }
    commitFragmentTransaction(FragmentExecutor.beginTransaction(mChildFm)
        .remove(fragment));
  }

  @Override
  public String getFragmentTAG() {
    return mFragmentTag;
  }

  @Override
  public int getContainerViewId() {
    return mContainerViewId;
  }

  /**
   * Throw the exception.
   */
  private void throwException(RiggerException e) {
    throw e;
  }

  /**
   * Commit fragment transaction.if the Activity is not resumed,then this transaction will be saved and commit as the
   * Activity is resumed.
   */
  private void commitFragmentTransaction(@NonNull Builder transaction) {
    if (isResumed()) {
      mFragmentTransactions.add(transaction);
      Logger.w(mFragment, "::Commit transaction---->The Activity is not resumed,the transaction will be saved");
    } else {
      transaction.commit();
    }
  }
}