package com.mintedtech.tic_tac_toe;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

enum WinType
{
    NONE, ROW, COLUMN, DIAGONAL
}

enum WinTypeDiagonal
{
    UPPER_LEFT_TO_LOWER_RIGHT, LOWER_LEFT_TO_UPPER_RIGHT
}

public class MainActivity extends AppCompatActivity
{
    // named constants (finals)
    private final int mEMPTY_SPACE = R.drawable.ic_xo_light, mINVALID_ICON_VALUE_FLAG = -99;
    private int mOLD_ICON_X, mOLD_ICON_O, mOLD_ICON_XO;

    // primitives and Strings
    private boolean mTurnX, mPrefUseAutoSave, mGameOver,
            mPrefComputerOpponent, mPrefComputerStarts;
    private String mLastGameResultsMessage, mLastWinner, mLastTurnResults;
    private int[] mWinningSpaces;
    private WinType mWinType;
    private WinTypeDiagonal mWinTypeDiagonal;
    private int mCurrentPosition, mPriorPosition = mINVALID_ICON_VALUE_FLAG;

    // These values are coded here rather than in strings.xml because they are not used elsewhere
    // If these keys might be read in another Activity then the values should instead be put in xml
    // Keys reference in both Java and XML - values stored in strings.xml
    private String mKEY_USE_AUTO_SAVE,
            mKEY_COMPUTER_OPPONENT, mKEY_COMPUTER_STARTS;

    // Keys referenced only in Java - values stored here
    private final String mKEY_PLAYER = "CURRENT_PLAYER";
    private final String mKEY_BOARD = "BOARD";
    private final String mKEY_TINTS = "TINTS";
    private final String mKEY_GAME_OVER = "GAME_OVER";
    private final String mKEY_LAST_TURN_RESULTS = "LAST_TURN_RESULTS";
    private final String mPREFS = "PREFS";
    private final String mKEY_CURRENT_POSITION = "PRIOR_POSITION";
    private final String mKEY_PRIOR_POSITION = "PRIOR_POSITION";
    private final String mKEY_LAST_RESULT = "LAST_RESULT";
    private final String mKEY_ICON_X = "ICON_X";
    private final String mKEY_ICON_O = "ICON_O";
    private final String mKEY_ICON_XO = "ICON_XO";

    // Reference to our custom Adapter used to create and maintain a board in our GridView here
    private CardViewImageAdapter mAdapter;

    // References to various Views
    private TextView mStatusBar;
    private ImageView mImageX, mImageO;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Snackbar mSbGame;
    private View mSbParentView;


    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);

        setContentView (R.layout.activity_main);

        initGUI ();

        createUnfilledBoard ();

        initializeSnackBar ();

        initializePreferences ();

        // If we are starting a fresh Activity (meaning, not after rotation), then do initial setup
        if (savedInstanceState == null) {
            setupInitialSession ();
        }
        // If we're in the middle of a game then onRestoreInstanceState will restore the App's state

    }

    private void initGUI ()
    {
        initializeStatusItems ();
        initializeSwipeRefreshLayout ();
    }

    /**
     * Initializes the members that are relevant to the status bar, like the x and o indicator icons
     * and the status bar (TextView)
     */
    private void initializeStatusItems ()
    {
        mImageX = findViewById (R.id.imageX);
        mImageO = findViewById (R.id.imageO);
        mStatusBar = findViewById (R.id.tv_status);
    }

    /**
     * This has what to do when the user swipes to refresh, including the anonymous inner-class
     */
    private void initializeSwipeRefreshLayout ()
    {
        mSwipeRefreshLayout = findViewById (R.id.swipe_refresh);
        mSwipeRefreshLayout.setOnRefreshListener (new SwipeRefreshLayout.OnRefreshListener ()
        {
            @Override
            public void onRefresh ()
            {
                prepareForNewGame ();
                startNewOrResumeGameState ();
            }
        });
    }

    /**
     * Creates an unfilled board, which includes creating the array of winning spaces (9/3 elements)
     * and the RecyclerView grid, including layout... and an instance of out custom adapter class.
     */
    private void createUnfilledBoard ()
    {
        final int TOTAL_SPACES = 9;

        // create an array to hold the winning spaces to send to adapter
        mWinningSpaces = new int[(int) (Math.sqrt (TOTAL_SPACES))];

        // Create the adapter for later use in the RecyclerView
        mAdapter = new CardViewImageAdapter (TOTAL_SPACES, R.drawable.ic_xo_light);

        // set the listener which will listen to the clicks in the RecyclerView
        mAdapter.setOnItemClickListener (listener);

        // get a reference to the RecyclerView
        RecyclerView board = findViewById (R.id.rv_board);
        assert board != null;

        // get a reference to a new LayoutManager for the RecyclerView
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager (this, 3);

        // set the adapter as the data source (model) for the RecyclerView
        board.setHasFixedSize (true);
        board.setLayoutManager (layoutManager);
        board.setAdapter (mAdapter);
    }

    /**
     * Only one SnackBar at a time is shown on the screen; if a new one comes up while one is there,
     * the new SB will replace the old one. So we can reuse our mSbGame referenced object
     * as opposed to requesting a new SB via its Static Factory method SnackBar.make(...)
     */
    private void initializeSnackBar ()
    {
        // Initialize (but do not show) SnackBar
        mSbParentView = findViewById (R.id.cl_main);
        assert mSbParentView != null;

        mSbGame = Snackbar.make (mSbParentView,
                                 mLastGameResultsMessage,
                                 Snackbar.LENGTH_INDEFINITE);
    }

    /**
     * These variables will hold the user's choices regarding auto-save and computer player/start
     */
    private void initializePreferences ()
    {
        mKEY_USE_AUTO_SAVE = getString (R.string.key_use_auto_save);
        mKEY_COMPUTER_OPPONENT = getString (R.string.key_computer_opponent);
        mKEY_COMPUTER_STARTS = getString (R.string.key_computer_starts);
    }

    /**
     * Called from onCreate() only if the user just entered the app from the home screen
     * as opposed to when destroying/recreating from e.g. an orientation change
     * <p/>
     * Steps:
     * 1. Done in SplashActivity - In case this is the first run ever, set the default values in shared prefs
     * 2. Then, set the last Game results message to first game of session
     * Next:
     * 3. Prepare for new game (but no initial computer turn)
     * 4. Restore all data from Shared Preferences
     * 5. Start or resume new game, meaning turn off animation
     * and conditionally take initial/next computer turn
     */
    private void setupInitialSession ()
    {
        mLastGameResultsMessage = getString (R.string.info_first_game_of_session);
        prepareForNewGame ();
        restoreAllDataFromPrefs ();
        startNewOrResumeGameState ();
    }

    private void prepareForNewGame ()
    {
        mAdapter.resetAllImagesAndTints ();

        resetGameAndTurnStatus ();

        resetCurrentAndPriorPositions ();

        dismissSnackBarIfShown ();
    }

    /**
     * Note: X always goes first, but the user may set either the human or computer to play first
     */
    private void resetGameAndTurnStatus ()
    {
        mGameOver = false;
        mLastTurnResults = "First turn of the game:";
        setCurrentPlayerToX (true);
    }

    private void setCurrentPlayerToX (boolean newValueOfX)
    {
        mTurnX = newValueOfX;

        updateTintOfImagesXO ();
        updateStatusBarWithCurrentTurn ();
    }

    private void updateTintOfImagesXO ()
    {
        int colorForLetterX = mTurnX ? R.color.color_yes : R.color.color_no;
        int colorForLetterO = mTurnX ? R.color.color_no : R.color.color_yes;

        mImageX.setColorFilter (ContextCompat.getColor (this, colorForLetterX));
        mImageO.setColorFilter (ContextCompat.getColor (this, colorForLetterO));
    }

    private void updateStatusBarWithCurrentTurn ()
    {
        String currentPlayer = getCurrentPlayer ();
        mStatusBar.setText (getString (R.string.current_turn).concat (currentPlayer));
    }

    /**
     * This data is for the SnackBar that is displayed at the end of each turn
     */
    private void resetCurrentAndPriorPositions ()
    {
        mPriorPosition = mINVALID_ICON_VALUE_FLAG;
        mCurrentPosition = mINVALID_ICON_VALUE_FLAG;
    }

    /**
     * When starting a new game, any game status, etc. from the old game should be dismissed
     */
    private void dismissSnackBarIfShown ()
    {
        if (mSbGame.isShown ()) {
            mSbGame.dismiss ();
        }
    }

    /**
     * If the user chose to Auto-Save and was in the middle of a game then board will be restored
     * Otherwise, since we just started a new game, the user will simply start a new game
     */
    private void restoreAllDataFromPrefs ()
    {
        restoreGameTypeAndAutoSaveStatus ();
        restoreLastStateIfAutoSaveIsOn ();
    }

    /**
     * This method essentially takes what was saved above and restores it.
     */
    private void restoreGameTypeAndAutoSaveStatus ()
    {
        // Since this is for reading only, no editor is needed unlike in saveRestoreState
        SharedPreferences preferences = getSharedPreferences (mPREFS, MODE_PRIVATE);

        // restore AutoSave preference value
        mPrefUseAutoSave = preferences.getBoolean (mKEY_USE_AUTO_SAVE, true);

        // restore user's opponent and start preferences
        mPrefComputerOpponent = preferences.getBoolean (mKEY_COMPUTER_OPPONENT, true);
        mPrefComputerStarts = preferences.getBoolean (mKEY_COMPUTER_STARTS, false);
    }

    private void restoreLastStateIfAutoSaveIsOn ()
    {
        SharedPreferences preferences = getSharedPreferences (mPREFS, MODE_PRIVATE);

        if (mPrefUseAutoSave) {

            // restore "game-over" state
            mGameOver = preferences.getBoolean (mKEY_GAME_OVER, false);

            // restore the last turn
            mLastTurnResults = preferences.getString (mKEY_LAST_TURN_RESULTS,
                                                      getString (
                                                              R.string.info_defaultValue_lastTurnResults));

            // restore the current and prior spaces
            mCurrentPosition = preferences.getInt (mKEY_CURRENT_POSITION, mINVALID_ICON_VALUE_FLAG);
            mPriorPosition = preferences.getInt (mKEY_PRIOR_POSITION, mINVALID_ICON_VALUE_FLAG);

            restoreAllBoardData (preferences);

            // restore current player
            setCurrentPlayerToX (preferences.getBoolean (mKEY_PLAYER, true));

            // Show Last Results
            mSbGame.setText (
                    mLastTurnResults.concat ("\n" + getString (R.string.info_game_resumed)));
        }
    }

    private void restoreAllBoardData (SharedPreferences preferences)
    {
        // restore the board icon values from SharedPreferences
        restoreBoardIcons (preferences);

        // restore the board from SharedPreferences
        restoreBoard (preferences);

        // restore the tints from SharedPreferences
        restoreTintsIfGameOver (preferences);
    }

    private void restoreBoard (SharedPreferences preferences)
    {
        String currentKeyName;
        int currentSpace;

        // restore the board one square at a time
        for (int i = 0; i < mAdapter.getItemCount (); i++) {
            currentKeyName = mKEY_BOARD + i;
            currentSpace = (int) preferences.getLong (currentKeyName, mEMPTY_SPACE);
            currentSpace = getValidCurrentSpace (currentSpace);
            mAdapter.setImage (i, currentSpace);
        }
    }

    private int getValidCurrentSpace (int currentSpace)
    {
        // The XO must be element #0 because the default pref value is empty space
        // So #0 will always match if the app is being run for the first time
        final int[] OLD_ICONS = new int[] {mOLD_ICON_XO, mOLD_ICON_X, mOLD_ICON_O,};
        final int[] CURRENT_ICONS =
                new int[] {R.drawable.ic_xo_light, R.drawable.ic_x, R.drawable.ic_o};

        int validIcon = mINVALID_ICON_VALUE_FLAG;

        for (int i = 0; i < CURRENT_ICONS.length && validIcon == mINVALID_ICON_VALUE_FLAG; i++) {
            validIcon = (currentSpace == OLD_ICONS[i] || currentSpace == CURRENT_ICONS[i])
                        ? CURRENT_ICONS[i] : validIcon;
        }

        return validIcon != mINVALID_ICON_VALUE_FLAG ? validIcon : R.drawable.ic_xo_light;
    }


    private void restoreTintsIfGameOver (SharedPreferences preferences)
    {
        // tints are not used unless the game is over
        if (mGameOver) {
            String currentKeyName;
            int currentSpaceTint;

            // restore the tints one square at a time
            for (int i = 0; i < mAdapter.getItemCount (); i++) {
                currentKeyName = mKEY_TINTS + i;
                currentSpaceTint =
                        (int) preferences.getLong (currentKeyName, mINVALID_ICON_VALUE_FLAG);
                currentSpaceTint = getValidCurrentSpaceTint (currentSpaceTint);
                mAdapter.setImageTint (i, currentSpaceTint);
            }
        }
    }

    /**
     * Returns a valid tint, meaning either an actual tint color ID or else the invalid flag value
     *
     * @param currentSpaceTint tint read in from SP
     * @return either the non-changing mInvalid_Icon_Value_Flag, which is -99 or else the color_yes
     */
    private int getValidCurrentSpaceTint (int currentSpaceTint)
    {
        return currentSpaceTint == mINVALID_ICON_VALUE_FLAG
               ? mINVALID_ICON_VALUE_FLAG
               : R.color.color_yes;
    }


    /**
     * Called as the last step in the new game process and when starting the app even with auto-save
     * 1. Always turns off the swipe-to-refresh animation
     * 2. If the game is not over (from last run, then closed/reopened) and it's the computer's turn
     */
    private void startNewOrResumeGameState ()
    {
        // regardless of how we got here (via listener, MenuItem click, etc), turn off animation
        mSwipeRefreshLayout.setRefreshing (false);

        // If the user chooses to have a computer opponent and that the computer should start (X)
        // and it is currently turn x (always first player)
        if (!mGameOver && mPrefComputerOpponent && mPrefComputerStarts & mTurnX) {
            doComputerTurnCycle ();
        }
    }

    private void restoreBoardIcons (SharedPreferences preferences)
    {
        mOLD_ICON_X = (int) preferences.getLong (mKEY_ICON_X, mEMPTY_SPACE);
        mOLD_ICON_O = (int) preferences.getLong (mKEY_ICON_O, mEMPTY_SPACE);
        mOLD_ICON_XO = (int) preferences.getLong (mKEY_ICON_XO, mEMPTY_SPACE);
    }

/*
    // ------------------------------------------------------------------------------------------
    // Handle Android life-cycle destroy/recreate so that user doesn't even know it was recreated
    // ------------------------------------------------------------------------------------------
*/


    /**
     * This method's Superclass implementation saves to its "Bundle" argument the values of Views
     * We will add to that bundle the variables that we need to persist across destroy/create cycles
     *
     * @param outState bundle containing the saved instance state
     */
    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState)
    {
        // save contents of views, etc. automatically
        super.onSaveInstanceState (outState);

        outState.putString (mKEY_LAST_RESULT, mLastGameResultsMessage);

        outState.putString (mKEY_LAST_TURN_RESULTS, mLastTurnResults);

        // save the "game over" state
        outState.putBoolean (mKEY_GAME_OVER, mGameOver);

        // save the user's choice of opponent and start
        outState.putBoolean (mKEY_COMPUTER_OPPONENT, mPrefComputerOpponent);
        outState.putBoolean (mKEY_COMPUTER_STARTS, mPrefComputerStarts);

        // save the current autoSave boolean
        outState.putBoolean (mKEY_USE_AUTO_SAVE, mPrefUseAutoSave);

        //save the current player
        outState.putBoolean (mKEY_PLAYER, mTurnX);

        //save the current and prior spaces chosen
        outState.putInt (mKEY_CURRENT_POSITION, mCurrentPosition);
        outState.putInt (mKEY_PRIOR_POSITION, mPriorPosition);

        // save the board layout as a whole to the bundle to be saved
        outState.putIntArray (mKEY_BOARD, mAdapter.getAllImages ());

        // save the tinting data model
        outState.putIntArray (mKEY_TINTS, mAdapter.getAllImageTints ());
    }

    /**
     * This method's Superclass implementation restore the values of Views, as above.
     * We will add to that the functionality to have it restore our variables that we saved above.
     *
     * @param savedInstanceState the bundle containing the saved instance state
     */
    @Override
    protected void onRestoreInstanceState (@NonNull Bundle savedInstanceState)
    {
        // Restore contents of views, etc. automatically
        super.onRestoreInstanceState (savedInstanceState);

        // restore autoSave
        mPrefUseAutoSave = savedInstanceState.getBoolean (mKEY_USE_AUTO_SAVE);

        mLastTurnResults = savedInstanceState.getString (mKEY_LAST_TURN_RESULTS);

        // restore the user's choice of opponent and start
        mPrefComputerOpponent = savedInstanceState.getBoolean (mKEY_COMPUTER_OPPONENT);
        mPrefComputerStarts = savedInstanceState.getBoolean (mKEY_COMPUTER_STARTS);

        // restore game over
        mGameOver = savedInstanceState.getBoolean (mKEY_GAME_OVER);

        // restore the results of the last game
        mLastGameResultsMessage = savedInstanceState.getString (mKEY_LAST_RESULT);

        // restore the current player
        setCurrentPlayerToX (savedInstanceState.getBoolean (mKEY_PLAYER));

        // restore the current and prior spaces
        mCurrentPosition = savedInstanceState.getInt (mKEY_CURRENT_POSITION);
        mPriorPosition = savedInstanceState.getInt (mKEY_PRIOR_POSITION);

        // restore the game board one space at a time
        restoreBoardFromSavedState (savedInstanceState);

        // show game over message if the current saved game had already ended
        showGameOverSnackBarIfGameOver ();
    }

    private void restoreBoardFromSavedState (Bundle savedInstanceState)
    {
        restoreBoardImageData (savedInstanceState);
        restoreBoardTintData (savedInstanceState);
    }


    private void restoreBoardImageData (Bundle savedInstanceState)
    {
        // This is essentially a copy of the board as it was before the rotation, etc.
        int[] existingBoard = savedInstanceState.getIntArray (mKEY_BOARD);

        if (existingBoard != null) {
            for (int i = 0; i < existingBoard.length; i++) {
                // since the Adapter's array is private, call the public setImage for each image
                mAdapter.setImage (i, existingBoard[i]);
            }
        }
    }


    private void restoreBoardTintData (Bundle savedInstanceState)
    {
        int[] existingTint = savedInstanceState.getIntArray (mKEY_TINTS);

        if (existingTint != null) {
            for (int i = 0; i < existingTint.length; i++) {
                if (existingTint[i] != mINVALID_ICON_VALUE_FLAG) {
                    mAdapter.setImageTint (i, R.color.color_yes);
                }
            }
        }
    }

    private void showGameOverSnackBarIfGameOver ()
    {
        if (mGameOver) {
            showGameOverSB (false);
        }
    }

/*
    // ------------------------------------------------------------------------------------------
    // Handle Android life-cycle onPause event
    // Specifically, in case the user swipes away the app (removes it from RAM),
    // Make sure the board and preferences have already been saved to SharedPreferences
    // ------------------------------------------------------------------------------------------
*/

    /**
     * In addition to the super-class's onPause, save the board to shared prefs now, just in case...
     */
    @Override
    protected void onPause ()
    {
        super.onPause ();
        savePrefAndBoardToSharedPref ();
    }

    private void savePrefAndBoardToSharedPref ()
    {
        // Create a SP object that (creates if needed and) uses the value of mPREFS as the file name
        SharedPreferences preferences = getSharedPreferences (mPREFS, MODE_PRIVATE);

        // Create an Editor object to write changes to the preferences object above
        SharedPreferences.Editor editor = preferences.edit ();

        // clear whatever was set last time
        editor.clear ();

        // save autoSave preference
        editor.putBoolean (mKEY_USE_AUTO_SAVE, mPrefUseAutoSave);

        // save opponent type and start preferences
        editor.putBoolean (mKEY_COMPUTER_OPPONENT, mPrefComputerOpponent);
        editor.putBoolean (mKEY_COMPUTER_STARTS, mPrefComputerStarts);

        // if autoSave is on then save the board
        saveBoardToSharedPrefsIfAutoSaveIsOn (editor);

        // apply the changes to the XML file in the device's storage
        editor.apply ();

    }

    private void saveBoardToSharedPrefsIfAutoSaveIsOn (SharedPreferences.Editor editor)
    {
        // (Only) if autoSave is enabled, then save the board and current player to the SP file
        if (mPrefUseAutoSave) {

            // save "game over" state
            editor.putBoolean (mKEY_GAME_OVER, mGameOver);

            // save last turn information
            editor.putString (mKEY_LAST_TURN_RESULTS, mLastTurnResults);

            // save current and prior spaces
            editor.putInt (mKEY_CURRENT_POSITION, mCurrentPosition);
            editor.putInt (mKEY_PRIOR_POSITION, mPriorPosition);

            saveAllBoardData (editor);

            // save current player (X or O) to SharedPreferences
            editor.putBoolean (mKEY_PLAYER, mTurnX);
        }
    }

    private void saveAllBoardData (SharedPreferences.Editor editor)
    {
        // save the board icon IDs to SharedPreferences
        saveBoardIcons (editor);

        // save the board to SharedPreferences
        saveBoard (editor);

        // save the tints to SharedPreferences (if gameOver)
        saveTints (editor);
    }

    /**
     * Stores the current IDs of the X, O and XO icons.
     * This is needed for auto-save purposes in case the IDs change between runs
     * such as if a new build is released
     *
     * @param editor The SharedPreferences Editor that saves the icons
     */
    private void saveBoardIcons (SharedPreferences.Editor editor)
    {
        editor.putLong (mKEY_ICON_X, R.drawable.ic_x);
        editor.putLong (mKEY_ICON_O, R.drawable.ic_o);
        editor.putLong (mKEY_ICON_XO, R.drawable.ic_xo_light);
    }


    private void saveBoard (SharedPreferences.Editor editor)
    {
        String currentKeyName;// save board one square at a time
        for (int i = 0; i < mAdapter.getItemCount (); i++) {
            currentKeyName = mKEY_BOARD + i;
            editor.putLong (currentKeyName, mAdapter.getItemId (i));
        }
    }

    private void saveTints (SharedPreferences.Editor editor)
    {

        // There are no tints unless the game has ended
        if (mGameOver) {
            int[] currentTint = mAdapter.getAllImageTints ();
            for (int i = 0; i < currentTint.length; i++) {
                editor.putLong (mKEY_TINTS + i, currentTint[i]);
            }
        }
    }

/*
    // ------------------------------------------------------------------------------------------
    // Handle Android menu framework for this application
    // Specifically, set each menu checkbox to the value matching their respective boolean's value
    // ------------------------------------------------------------------------------------------
*/

    @Override
    public boolean onPrepareOptionsMenu (Menu menu)
    {
        super.onPrepareOptionsMenu (menu);

        // prepares the value for the mPrefUseAutoSave checked item in the menu
        // meaning: check or remove the check in the menu to match the user value for this pref.
        menu.findItem (R.id.action_autoSave).setChecked (mPrefUseAutoSave);
        menu.findItem (R.id.action_computerOpponent).setChecked (mPrefComputerOpponent);
        menu.findItem (R.id.action_computerStarts).setChecked (mPrefComputerStarts);
        return super.onPrepareOptionsMenu (menu);
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater ().inflate (R.menu.menu_main, menu);
        return super.onCreateOptionsMenu (menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId ();

        switch (id) {

            case R.id.action_newGame: {

                startNewGameIncludingSRAnimation ();
                return true;
            }

            case R.id.action_autoSave: {

                toggleItemCheck (item);
                mPrefUseAutoSave = item.isChecked ();
                return true;
            }
            case R.id.action_computerOpponent: {

                toggleItemCheck (item);
                mPrefComputerOpponent = item.isChecked ();
                doComputerTurnCycleIfCheckedAndNotGameOverAndIsComputerTurn ();
                return true;
            }
            case R.id.action_computerStarts: {

                toggleItemCheck (item);
                mPrefComputerStarts = item.isChecked ();
                doComputerTurnCycleIfCheckedAndNotGameOverAndIsComputerTurn ();
                return true;
            }
            case R.id.action_about: {
                showTTTDialog (getString (R.string.aboutDialogTitle),
                               getString (R.string.aboutDialog_banner)
                );
                return true;
            }
        }
        return super.onOptionsItemSelected (item);
    }

    private void startNewGameIncludingSRAnimation ()
    {
        // start animation
        mSwipeRefreshLayout.setRefreshing (true);
        prepareForNewGame ();
        startNewOrResumeGameState ();
    }


    private void toggleItemCheck (MenuItem item)
    {
        item.setChecked (!item.isChecked ());
    }

    private void doComputerTurnCycleIfCheckedAndNotGameOverAndIsComputerTurn ()
    {
        if (mPrefComputerOpponent && !mGameOver &&
                (mPrefComputerStarts && mTurnX || !mPrefComputerStarts && !mTurnX)) {
            doComputerTurnCycle ();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // This method is called from showAbout (MenuItem item), specified in the item's onClick in XML.
    // It is also called from the Game Over code sequence
    //
    // Note: To use the new "Material Design"-look AlertDialog, the AlertDialog's import should be:
    // import android.support.v7.app.AlertDialog;
    // ---------------------------------------------------------------------------------------------
    private void showTTTDialog (String title, String message)
    {
        // Create listener for use with dialog window (could also be created anonymously below...
        DialogInterface.OnClickListener dialogOnClickListener =
                createTTTOnClickListener ();

        // Create dialog window
        AlertDialog TTTAlertDialog = initDialog (title, message, dialogOnClickListener);

        // Show the dialog window
        TTTAlertDialog.show ();

    }

    private AlertDialog initDialog (String title, String message,
                                    DialogInterface.OnClickListener dialogOnClickListener)
    {
        AlertDialog TTTAlertDialog;
        TTTAlertDialog = new AlertDialog.Builder (MainActivity.this).create ();
        TTTAlertDialog.setTitle (title);
        TTTAlertDialog.setIcon (R.mipmap.ic_launcher);
        TTTAlertDialog.setMessage (message);
        TTTAlertDialog.setButton (DialogInterface.BUTTON_NEUTRAL,
                                  getString (R.string.OK), dialogOnClickListener);
        return TTTAlertDialog;
    }

    private DialogInterface.OnClickListener createTTTOnClickListener ()
    {
        return new DialogInterface.OnClickListener ()
        {
            @Override
            public void onClick (DialogInterface dialog, int which)
            {
                // nothing to do
            }
        };
    }

    /**
     * This object of our custom Listener class handles events in the adapter; created here anon.
     * This leaves the Adapter to handle the Model part of MVC, not View or Controller
     */
    private final CardViewImageAdapter.OIClickListener
            listener = new CardViewImageAdapter.OIClickListener ()
    {
        public void onItemClick (int position, View view)
        {
            // if the game is already over then there is nothing more to do here
            if (mGameOver) {
                showGameOverSB (true);
            }
            // If the current space is empty and, therefore available and thus a valid space
            else if (isSpaceEmpty (position)) {
                processClickOnValidSpace (position);
            }
            else {
                showInvalidSpaceSB ();
            }
        }
    };

    private boolean isSpaceEmpty (int position)
    {
        return mAdapter.getItemId (position) == mEMPTY_SPACE;
    }

    private void processClickOnValidSpace (int position)
    {
        doHumanTurnCycle (position);

        if (mPrefComputerOpponent && !mGameOver) {
            if (mPrefComputerStarts && mTurnX || !mPrefComputerStarts && !mTurnX) {
                doComputerTurnCycle ();
            }
            else {
                Toast.makeText (getApplicationContext (),
                                R.string.info_computer_goes_next, Toast.LENGTH_SHORT).show ();
            }
        }
    }

    private void doHumanTurnCycle (int position)
    {
        // process this turn/move
        doPlayerTurn (position);

        showTurnStatus (position);

        // check for win and/or full board
        doPostPlayerTurn ();
    }

    /**
     * Switches changes the board's space icon in that space to match the X or O
     * and updates the current and recent positions to match the just-chosen space
     *
     * @param position The position on the board to change to X or O
     */
    private void doPlayerTurn (final int position)
    {
        // change the icon at that position from empty to either X or O as appropriate
        mAdapter.setImage (position, getIconForCurrentPlayer ());
        updateMemberPositions (position);
    }


    private int getIconForCurrentPlayer ()
    {
        // Reference to X or O, depending on value of mTurnX (which player's turn it is)
        return mTurnX ? R.drawable.ic_x : R.drawable.ic_o;
    }

    private void updateMemberPositions (int position)
    {
        mPriorPosition = mCurrentPosition;
        mCurrentPosition = position;
    }


    private void showTurnStatus (final int position)
    {
        String strPosition = getOneBasedRowAndColumnAt (position);
        showRowAndColumnAt (position, strPosition);
    }

    @NonNull
    private String getOneBasedRowAndColumnAt (int position)
    {
        int totalSpaces = mAdapter.getItemCount ();
        int rowsAndColumns = (int) Math.sqrt (totalSpaces);

        int oneBasePosition = position + 1;
        int row = (oneBasePosition / rowsAndColumns) + 1;
        int col = oneBasePosition % rowsAndColumns;

        if (col == 0) {
            row--;
            col = rowsAndColumns;
        }

        return getString (R.string.row_colon) + row + ", " + getString (
                R.string.column_colon) + col;
    }

    private void showRowAndColumnAt (final int position, String strPosition)
    {
        String msg = getCurrentPlayer () + getString (R.string.chose) + strPosition + '.';

        // Create SnackBar with status message of this past turn
        mSbGame =
                Snackbar.make (mSbParentView, mLastTurnResults + "\n" + msg, Snackbar.LENGTH_LONG);
        mLastTurnResults = msg;

        // Allow and setup undo
        mSbGame.setAction ("Undo", new View.OnClickListener ()
        {
            @Override
            public void onClick (View v)
            {
                undoLastMove (position);
            }
        });

        // Show SnackBar
        mSbGame.show ();
    }

    @NonNull
    private String getCurrentPlayer ()
    {
        return mTurnX ? getString (R.string.x) : getString (R.string.o);
    }

    private void undoLastMove (int position)
    {
        if (!mPrefComputerOpponent) {
            mAdapter.setImage (position, R.drawable.ic_xo_light);
            setCurrentPlayerToX (!mTurnX);
        }
        else {
            if (mCurrentPosition != mINVALID_ICON_VALUE_FLAG && mPriorPosition != mINVALID_ICON_VALUE_FLAG) {
                mAdapter.setImage (mCurrentPosition, R.drawable.ic_xo_light);
                mAdapter.setImage (mPriorPosition, R.drawable.ic_xo_light);
            }
            else if (mPriorPosition == mINVALID_ICON_VALUE_FLAG) {
                mSbGame.setText (R.string.error_cannot_undo_this_move).setDuration (
                        Snackbar.LENGTH_SHORT).show ();
            }
        }
    }

    private void doPostPlayerTurn ()
    {
        if (isGameOver ()) {
            doGameOverTasks ();
        }
        else {
            // flip mTurnX between X and O; meaning, set the turn to be the other player's turn
            setCurrentPlayerToX (!mTurnX);
        }
    }

    private void doGameOverTasks ()
    {
        mGameOver = true;
        generateGameResults ();
        tintWinningSpacesIfNotDraw ();
        showGameOverSB (false);
    }

    private void generateGameResults ()
    {
        if (mLastWinner.equals (getString (R.string.no_winner))) {
            mLastGameResultsMessage = getString (R.string.info_board_full);
        }
        else {
            final int ROW_COL_LENGTH = (int) Math.sqrt (mAdapter.getItemCount ());
            mLastGameResultsMessage = getWinningRowColumnOrDiagonalMessage (ROW_COL_LENGTH);
        }
    }

    @NonNull
    private StringBuilder generateGameOverMessage (boolean gameAlreadyOver)
    {
        StringBuilder sbText = new StringBuilder (getString (R.string.info_game_over));
        sbText.append (' ');

        if (gameAlreadyOver) {
            sbText.append (getString (R.string.info_game_already_over));
        }
        else {
            sbText.append (mLastGameResultsMessage);
        }
        return sbText;
    }

    private void tintWinningSpacesIfNotDraw ()
    {
        if (!mLastWinner.equals (getString (R.string.no_winner))) {
            mAdapter.setAllImagesTint (mWinningSpaces, R.color.color_yes);
        }
    }

    private void showGameOverSB (boolean gameAlreadyOver)
    {
        StringBuilder sbText = generateGameOverMessage (gameAlreadyOver);
        mSbGame = Snackbar.make (mSbParentView, sbText, Snackbar.LENGTH_INDEFINITE);
        mSbGame.setAction (R.string.action_newGame, new View.OnClickListener ()
        {
            @Override
            public void onClick (View v)
            {
                startNewGameIncludingSRAnimation ();
            }
        })
                .show ();
    }

    private void showInvalidSpaceSB ()
    {
        Snackbar.make (mSbParentView,
                       getString (R.string.error_space_already_taken),
                       Snackbar.LENGTH_SHORT).show ();
    }

    private int[] getAvailableSpaces ()
    {
        ArrayList<Integer> availableSpacesList = new ArrayList<> ();
        int[] availableSpacesArray;

        for (int i = 0; i < mAdapter.getItemCount (); i++) {
            if (isSpaceEmpty (i)) {
                availableSpacesList.add (i);
            }
        }

        availableSpacesArray = new int[availableSpacesList.size ()];
        for (int i = 0; i < availableSpacesArray.length; i++) {
            availableSpacesArray[i] = availableSpacesList.get (i);
        }

        return availableSpacesArray;
    }

    private void doComputerTurnCycle ()
    {
        int computerPosition = doComputerTurn ();
        showTurnStatus (computerPosition);
        doPostPlayerTurn ();
    }

    private int doComputerTurn ()
    {
        Random generator = new Random ();
        int[] spaces = getAvailableSpaces ();
        int length = spaces.length;
        int element = generator.nextInt (length);
        int position = spaces[element];

        doPlayerTurn (position);
        return position;
    }

    private boolean isGameOver ()
    {
        // Assume game is over until proven otherwise
        boolean gameOver = true;

        if (isWinner ()) {
            mLastWinner = mTurnX ? getString (R.string.x) : getString (R.string.o);
        }
        else if (isBoardFull ()) {
            mLastWinner = getString (R.string.no_winner);
        }
        else {
            gameOver = false;
        }
        return gameOver;
    }

    /**
     * Method determines of the game has been won by either player
     *
     * @return true if there is a winner; false otherwise
     */
    private boolean isWinner ()
    {
        boolean winner;
        int totalCells = mAdapter.getItemCount ();
        int rowColLength = (int) Math.sqrt (totalCells); //3

        winner = checkIfRowColumnOrDiagonalWinner (totalCells, rowColLength);

        return winner;
    }

    /**
     * Method determines if the board has already been filled up; no places left to click
     *
     * @return true if board is full
     */
    private boolean isBoardFull ()
    {
        int count = mAdapter.getItemCount (); //9
        boolean emptySpaceFound = false;

        for (int i = 0; i < count && !emptySpaceFound; i++) {
            emptySpaceFound = isSpaceEmpty (i);
        }
        return !emptySpaceFound;
    }

    /**
     * Generates the message to be outputted to the user regarding who won and by which direction
     * (e.g. Computer won; winning row number is: 1)
     *
     * @param rowColLength How many rows and columns are in the grid
     * @return the message to be outputted to the user
     */
    @NonNull
    private String getWinningRowColumnOrDiagonalMessage (int rowColLength)
    {
        int rowColDivideValue = mWinType.equals (WinType.ROW) ? rowColLength : 1;

        return getCurrentPlayer () + getString (R.string.info_has_won) +
                "\nWinning " +
                mWinType.toString ().toLowerCase () +
                (mWinType.equals (WinType.DIAGONAL) ? ": " : " number: ") +
                (mWinType.equals (WinType.DIAGONAL) ?
                 mWinTypeDiagonal.toString ().toLowerCase ().replace ('_', ' ') :
                 (mWinningSpaces[0] / rowColDivideValue) + 1) +
                '.';
    }

    private boolean checkIfRowColumnOrDiagonalWinner (int totalCells, int rowColLength)
    {
        boolean rowWinner, colWinner = false, diagonalWinner = false;

        // check row; if not winner, then check column; if still not winner, then check diagonals
        rowWinner = isRowWinner (totalCells, rowColLength);
        mWinType = rowWinner ? WinType.ROW : WinType.NONE;

        if (!rowWinner) {
            colWinner = isColWinner (totalCells, rowColLength);
            mWinType = colWinner ? WinType.COLUMN : WinType.NONE;

            if (!colWinner) {
                diagonalWinner = isDiagonalWinner (totalCells, rowColLength);
                mWinType = diagonalWinner ? WinType.DIAGONAL : WinType.NONE;
            }
        }

        return rowWinner || colWinner || diagonalWinner;
    }

    private boolean isRowWinner (int totalCells, int rowColLength)
    {
        boolean rowWinner = false;
        int currentSpaceImage, currentStartingSpace = 0;
        // check row of 1-dimensional array
        for (int i = 0, rowCurrentStartingSpaceImage; i < totalCells && !rowWinner;
             i += rowColLength) {

            // set current space and also assume the row will be a winner if starting space != empty
            rowCurrentStartingSpaceImage = (int) mAdapter.getItemId (i);
            rowWinner = rowCurrentStartingSpaceImage != mEMPTY_SPACE;

            // If this is not an empty space, then assume rowWinner will be true
            if (rowWinner) {
                for (int j = i + 1; j < i + rowColLength; j++) {
                    currentStartingSpace = j;
                    currentSpaceImage = (int) mAdapter.getItemId (j);
                    if (currentSpaceImage != rowCurrentStartingSpaceImage) {
                        rowWinner = false;
                    }
                }
                setWinningSpacesIfRowColWinner (rowColLength, rowWinner,
                                                (currentStartingSpace - rowColLength) + 1, 1);
            }
        }
        return rowWinner;
    }


    private boolean isColWinner (int totalCells, int rowColLength)
    {
        boolean colWinner = false;
        int currentSpaceImage, currentStartingSpace = 0;
        // check col of 1-dimensional array
        for (int i = 0, colCurrentStartingSpaceImage; i < rowColLength && !colWinner; i++) {

            // set current space and also assume the col will be a winner if not empty space
            colCurrentStartingSpaceImage = (int) mAdapter.getItemId (i);
            colWinner = colCurrentStartingSpaceImage != mEMPTY_SPACE;

            // If this is not an empty space, then assume colWinner will be true
            if (colWinner) {
                for (int j = i + rowColLength; j < totalCells; j += rowColLength) {
                    currentStartingSpace = j - rowColLength;
                    currentSpaceImage = (int) mAdapter.getItemId (j);
                    if (currentSpaceImage != colCurrentStartingSpaceImage) {
                        colWinner = false;
                    }
                }
                setWinningSpacesIfRowColWinner (rowColLength, colWinner,
                                                currentStartingSpace - rowColLength, rowColLength);
            }
        }
        return colWinner;
    }

    // Used in both isRowWinner and isColWinner
    private void setWinningSpacesIfRowColWinner (int rowAndColLength, boolean rowOrColWinner,
                                                 int rowOrColCurrentStartingSpace, int stepBy)
    {
        // after going through this row or column
        if (rowOrColWinner) {
            for (int k = 0; k < rowAndColLength; k++) {
                mWinningSpaces[k] = rowOrColCurrentStartingSpace + (k * stepBy);
            }
        }
    }

    private boolean isDiagonalWinner (int totalCells, int rowColLength)
    {
        boolean diagonalWinner;
        int diagonalStart;

        // do the first diagonal check (Upper-Left to Lower-Right)
        diagonalStart = 0;
        diagonalWinner = isDiagonalWinnerULLR (totalCells, rowColLength, diagonalStart);

        // if the first diagonal check (Upper-Left to Lower-Right) passed
        if (diagonalWinner) {
            setWinnerMemberDataULLR (rowColLength, diagonalStart);
        }
        else {   // do the second diagonal check (Lower-Left to Upper-Right) passes
            diagonalStart = rowColLength - 1;
            diagonalWinner = isDiagonalWinnerLLUR (totalCells, rowColLength, diagonalStart);

            // if the second diagonal check passes
            if (diagonalWinner) {
                setWinnerMemberDataLLUR (rowColLength, diagonalStart);
            }
        }

        return diagonalWinner;
    }

    private void setWinnerMemberDataLLUR (int rowColLength, int diagonalStart)
    {
        for (int i = rowColLength - 1; i >= 0; i--) {
            mWinningSpaces[i] = diagonalStart - i + (rowColLength * i);
        }
        mWinTypeDiagonal = WinTypeDiagonal.LOWER_LEFT_TO_UPPER_RIGHT;
    }

    private void setWinnerMemberDataULLR (int rowColLength, int diagonalStart)
    {
        for (int i = 0; i < rowColLength; i++) {
            mWinningSpaces[i] = diagonalStart + i + (i * rowColLength);
        }
        mWinTypeDiagonal = WinTypeDiagonal.UPPER_LEFT_TO_LOWER_RIGHT;

    }

    private boolean isDiagonalWinnerLLUR (int totalCells, int rowColLength, int diagonalStart)
    {
        int currentStartingSpace;
        boolean diagonalWinner;
        int currentSpace;
        currentStartingSpace = (int) mAdapter.getItemId (diagonalStart);
        diagonalWinner = currentStartingSpace != mEMPTY_SPACE;

        if (diagonalWinner) {
            for (int i = rowColLength - 1;
                 i <= totalCells - rowColLength;
                 i += rowColLength - 1) {
                currentSpace = (int) mAdapter.getItemId (i);

                if (currentSpace != currentStartingSpace) {
                    diagonalWinner = false;
                }
            }
        }
        return diagonalWinner;
    }

    private boolean isDiagonalWinnerULLR (int totalCells, int rowColLength, int diagonalStart)
    {
        int currentStartingSpace;
        boolean diagonalWinner;
        int currentSpace;
        currentStartingSpace = (int) mAdapter.getItemId (diagonalStart);

        diagonalWinner = currentStartingSpace != mEMPTY_SPACE;

        if (diagonalWinner) {
            for (int i = 0; i < totalCells; i += rowColLength + 1) {
                currentSpace = (int) mAdapter.getItemId (i);

                if (currentSpace != currentStartingSpace) {
                    diagonalWinner = false;
                }
            }
        }
        return diagonalWinner;
    }


    // ---------------------------------------------------------------------------------------------
    // LG work-around to prevent crash when user hits menu button
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_MENU) &&
                (Build.VERSION.SDK_INT <= 16) &&
                (Build.MANUFACTURER.compareTo ("LGE") == 0)) {
            Log.i ("LG", "LG Legacy Device Detected");
            return true;
        }
        else {
            return super.onKeyDown (keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp (int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_MENU) &&
                (Build.VERSION.SDK_INT <= 16) &&
                (Build.MANUFACTURER.compareTo ("LGE") == 0)) {
            openOptionsMenu ();
            return true;
        }
        else {
            return super.onKeyUp (keyCode, event);
        }
    }

}