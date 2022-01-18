package com.mintedtech.tic_tac_toe.models;

import com.google.gson.Gson;
import com.mintedtech.tic_tac_toe.enums.PlayerTurn;
import com.mintedtech.tic_tac_toe.enums.WinType;
import com.mintedtech.tic_tac_toe.enums.WinTypeDiagonal;

import java.util.Arrays;

public class TicTacToe
{
    private final PlayerTurn[][] mBoardGrid;
    private final boolean[][] mWinningSpaces;

    private PlayerTurn mCurrentPlayer;
    private WinType mWinType;
    private WinTypeDiagonal mWinTypeDiagonal;

    private final int mTOTAL_ROWS_OR_COLUMNS;

    private final PlayerTurn mFirstPlayer = PlayerTurn.values ()[1];       // 0 is NONE
    private final PlayerTurn mLastPlayer = PlayerTurn.values ()[PlayerTurn.values ().length - 1];

    private boolean mGameOver; // convenience variable to prevent multiple expensive board checks

    // Undo
    private int mCurrentColumn, mPriorColumn, mCurrentRow,  mPriorRow;
    private boolean mCanUndo;

    // Stats
    private final int[] mWinCount;
    private int mWinningRowOrColumn;
    public int mNumberOfGamesPlayed;

    public TicTacToe (int totalRowsOrColumns)
    {
        mTOTAL_ROWS_OR_COLUMNS = totalRowsOrColumns;

        this.mBoardGrid = new PlayerTurn[totalRowsOrColumns][totalRowsOrColumns];
        this.mWinningSpaces = new boolean[totalRowsOrColumns][totalRowsOrColumns];
        this.mWinCount = new int[PlayerTurn.values ().length];
        this.mNumberOfGamesPlayed = 0;

        this.mCurrentPlayer = PlayerTurn.None;

        startGame ();
    }

    public void startGame ()
    {
        mGameOver = false;
        mCanUndo = false;
        setupBoardForNewGame ();
        setWinningSpacesArrayToAllFalse ();

        mWinType = WinType.NONE;
        mWinTypeDiagonal = WinTypeDiagonal.NONE;
        mWinningRowOrColumn = -1;

        mCurrentPlayer = PlayerTurn.X;
    }

    private void endCurrentGame ()
    {
        mGameOver=true;
        if (isWinner ())
            mWinCount[mCurrentPlayer.ordinal ()]++;

        mNumberOfGamesPlayed++;
        setWinTypesAndSpaces ();
    }

    private void setWinTypesAndSpaces ()
    {
        if (isRowWinner ()) {
            mWinType = WinType.ROW;
            setSpacesRowWinner ();
        }
        else if (isColWinner ()) {
            mWinType = WinType.COLUMN;
            setSpacesColWinner ();
        }
        else {
            mWinType = WinType.DIAGONAL;

            if (isDiagonalTopLeftWinner ()) {
                mWinTypeDiagonal = WinTypeDiagonal.UPPER_LEFT_TO_LOWER_RIGHT;
                setSpacesDiagonalTopLeftWinner ();
            }
            else if (isDiagonalTopRightWinner ()) {
                mWinTypeDiagonal = WinTypeDiagonal.LOWER_LEFT_TO_UPPER_RIGHT;
                setSpacesDiagonalTopRightWinner ();
            }
            else
            {
                mWinType = WinType.NONE;
            }
        }

    }

    public WinType getWinType ()
    {
        return mWinType;
    }

    public WinTypeDiagonal getWinTypeDiagonal ()
    {
        return mWinTypeDiagonal;
    }

    private void setupBoardForNewGame ()
    {
        for (int i = 0; i < mTOTAL_ROWS_OR_COLUMNS; i++) {
            for (int j = 0; j < mTOTAL_ROWS_OR_COLUMNS; j++) {
                setSpaceXYToPlayer (i, j, PlayerTurn.None);
            }
        }
    }

    private void setSpaceXYToPlayer (int row, int col, PlayerTurn turnPlayer)
    {
        mBoardGrid[row][col] = turnPlayer;
    }

    public void attemptTurn (int row, int col)
    {
        if (isValidClick (row, col)) {
            updateUndoStatus(row, col);
            setSpaceXYToPlayer (row, col, mCurrentPlayer);
            doNextPlayerOrEndGame ();
        }
        else {
            String errorMessage = "Cannot set row " + row + ", col " + col + " to " + mCurrentPlayer;
            throw new IllegalArgumentException (errorMessage);
        }
    }

    private void updateUndoStatus (int row, int col)
    {
        mCanUndo = true;

        mPriorColumn = mCurrentColumn;
        mPriorRow = mCurrentRow;

        mCurrentRow = row;
        mCurrentColumn = col;
    }

    private void doNextPlayerOrEndGame ()
    {
        if (isBoardFull () || isWinner()) {
            endCurrentGame ();
        }
        else {
            gotoNextPlayer ();
        }
    }

    private void gotoNextPlayer ()
    {
        mCurrentPlayer = mCurrentPlayer == mLastPlayer ? mFirstPlayer :
                         PlayerTurn.values ()[mCurrentPlayer.ordinal () + 1];
    }

    public PlayerTurn getPlayerAtPosition (int row, int col)
    {
        return mBoardGrid[row][col];
    }

    public PlayerTurn[][] getCurrentBoardGrid ()
    {
        return mBoardGrid.clone (); //Arrays.copyOf (boardGrid, boardGrid.length);
    }

    public PlayerTurn getCurrentPlayer ()
    {
        return mCurrentPlayer;
    }

    public int getNumberOfGamesPlayed ()
    {
        return mNumberOfGamesPlayed;
    }

    public int getNumberOfWinsForPlayer (PlayerTurn pTurn)
    {
        return mWinCount[pTurn.ordinal ()];
    }

    public PlayerTurn getPriorPlayer ()
    {
        return mCurrentPlayer == mFirstPlayer ?
               mLastPlayer : PlayerTurn.values ()[mCurrentPlayer.ordinal () - 1];
    }

    public boolean[][] getWinningSpaces ()
    {
        return mWinningSpaces.clone ();
    }

    public int getCurrentColumn ()
    {
        return mCurrentColumn;
    }

    public int getPriorColumn ()
    {
        return mPriorColumn;
    }

    public int getCurrentRow ()
    {
        return mCurrentRow;
    }

    public int getPriorRow ()
    {
        return mPriorRow;
    }

    public boolean isCanUndo ()
    {
        return mCanUndo;
    }

    public void resetStatistics ()
    {
        mNumberOfGamesPlayed = 0;
        Arrays.fill (mWinCount,0);
    }

    public void undoLastTurn ()
    {
        if (mCanUndo)
        {
            mBoardGrid[mCurrentRow][mCurrentColumn] = PlayerTurn.None;

            mCurrentRow = mPriorRow;
            mCurrentColumn = mPriorColumn;

            if (mGameOver)
                setWinningSpacesArrayToAllFalse ();
            else
                mCurrentPlayer = getPriorPlayer ();

            mGameOver = false;
            mCanUndo = false;
        }
    }

    public int getWinningRowOrColumn ()
    {
        return mWinningRowOrColumn;
    }

    public boolean isValidClick (int row, int col)
    {
        return mBoardGrid[row][col].equals (PlayerTurn.None);
    }

    public boolean isGameOver ()
    {
        return mGameOver; //(isBoardFull () || isWinner ());
    }

    public boolean isBoardFull ()
    {
        for (PlayerTurn[] playerTurns : mBoardGrid) {
            for (PlayerTurn playerTurn : playerTurns) {
                if (playerTurn.equals (PlayerTurn.None)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isWinner ()
    {
        return (isRowWinner () || isColWinner ()
                        || isDiagonalTopLeftWinner () || isDiagonalTopRightWinner ());
    }

    private boolean isDiagonalTopRightWinner ()
    {
        PlayerTurn currentDiagonalValue;
        boolean isDiagonalWinner = false;

        int shortestRowLength = getShortestRowLength ();

        currentDiagonalValue = mBoardGrid[0][shortestRowLength - 1];
        if (!currentDiagonalValue.equals (PlayerTurn.None)) {
            // assume diagonal will be all the same, for now
            isDiagonalWinner = true;
            for (int i = 1; i < mBoardGrid.length; i++) {
                if (!mBoardGrid[i][shortestRowLength - 1 - i].equals (currentDiagonalValue)) {
                    isDiagonalWinner = false;
                    break;
                }
            }
        }
        return isDiagonalWinner;
    }

    private boolean isDiagonalTopLeftWinner ()
    {
        PlayerTurn currentDiagonalValue;
        boolean isDiagonalWinner = false;

        currentDiagonalValue = mBoardGrid[0][0];
        if (!currentDiagonalValue.equals (PlayerTurn.None)) {
            // assume diagonal will be all the same, for now
            isDiagonalWinner = true;
            for (int i = 1; i < mBoardGrid.length; i++) {
                if (!mBoardGrid[i][i].equals (currentDiagonalValue)) {
                    isDiagonalWinner = false;
                    break;
                }
            }
        }
        return isDiagonalWinner;
    }

    private boolean isRowWinner ()
    {
        PlayerTurn currentRowValue;
        boolean currentRowWinner;
        for (int i = 0; i < mBoardGrid.length; i++) {
            currentRowValue = mBoardGrid[i][0];
            if (!currentRowValue.equals (PlayerTurn.None)) {
                // assume row will be all the same, for now
                currentRowWinner = true;

                // check row starting from the second cell
                for (int j = 1; j < mBoardGrid[i].length; j++) {
                    if (!mBoardGrid[i][j].equals (currentRowValue)) {
                        currentRowWinner = false;
                        break;
                    }

                }
                // by the end of the row, if we haven't changed to false then the row matches!
                if (currentRowWinner) {
                    mWinningRowOrColumn = i;
                    return true;
                }
                // else - check the next row, if any
            }
        }
        return false;
    }

    private boolean isColWinner ()
    {
        PlayerTurn currentColValue;
        boolean currentColWinner;

        int shortestRowLength = getShortestRowLength ();

        for (int j = 0; j < shortestRowLength; j++) {
            currentColValue = mBoardGrid[0][j];
            if (!currentColValue.equals (PlayerTurn.None)) {
                // assume col will be all the same, for now
                currentColWinner = true;

                // check col starting from the second cell
                for (int i = 1; i < mBoardGrid.length; i++) {
                    if (!mBoardGrid[i][j].equals (currentColValue)) {
                        currentColWinner = false;
                        break;
                    }
                }
                // by the end of the col, if we haven't changed to false then the col matches!
                if (currentColWinner) {
                    mWinningRowOrColumn = j;
                    return true;
                }
                // else - check the next col, if any
            }
        }
        return false;
    }

    private int getShortestRowLength ()
    {
        int shortestRowLength = Integer.MAX_VALUE;
        for (PlayerTurn[] playerTurns : mBoardGrid) {
            shortestRowLength = Math.min (playerTurns.length, shortestRowLength);
        }
        return shortestRowLength;
    }

    private void setWinningSpacesArrayToAllFalse ()
    {
        for (boolean[] winSpace : mWinningSpaces) {
            Arrays.fill (winSpace, false);
        }
    }

    private void setSpacesDiagonalTopRightWinner ()
    {
        int shortestRowLength = getShortestRowLength ();

        mWinningSpaces[0][shortestRowLength - 1] = true;
        for (int i = 1; i < mBoardGrid.length; i++) {
            mWinningSpaces[i][shortestRowLength - 1 - i] = true;
        }
    }

    private void setSpacesDiagonalTopLeftWinner ()
    {
        for (int i = 0; i < mBoardGrid.length; i++) {
            mWinningSpaces[i][i] = true;
        }
    }

    private void setSpacesRowWinner ()
    {
        for (int j = 0; j < mBoardGrid[mWinningRowOrColumn].length; j++) {
            mWinningSpaces[mWinningRowOrColumn][j] = true;
        }
    }

    private void setSpacesColWinner ()
    {
        for (int i = 0; i < mBoardGrid.length; i++) {
            mWinningSpaces[i][mWinningRowOrColumn] = true;
        }
    }

    /**
     * Reverses the game object's serialization as a String
     * back to a TTT game object
     *
     * @param json The serialized String of the game object
     * @return The game object
     */
    public static TicTacToe getGameFromJSON (String json)
    {
        Gson gson = new Gson ();
        return gson.fromJson (json, TicTacToe.class);
    }

    /**
     * Serializes the game object to a JSON-formatted String
     *
     * @param obj Game Object to serialize
     * @return JSON-formatted String
     */
    public static String getJSONFromGame (TicTacToe obj)
    {
        Gson gson = new Gson ();
        return gson.toJson (obj);
    }

    public String getJSONFromCurrentGame ()
    {
        return getJSONFromGame (this);
    }
}
