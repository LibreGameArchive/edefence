package com.calefay.exodusDefence;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme.app.AbstractGame;
import com.jme.input.InputSystem;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;
import com.jme.util.ThrowableHandler;

public abstract class EDBaseGame {
    private static final Logger logger = Logger.getLogger(EDBaseGame.class
            .getName());
	protected ThrowableHandler throwableHandler;

    /** Flag for running the system. */
    protected boolean finished;


    /** Renderer used to display the game */
    protected DisplaySystem display;

    /**
     * The simplest main game loop possible: render and update as fast as
     * possible.
     */
    public final void start() {
        logger.info( "Application started.");
        try {

            if (!finished) {
                initSystem();

                assertDisplayCreated();

                initGame();

                // main loop
                while (!finished && !display.isClosing()) {
                    // handle input events prior to updating the scene
                    // - some applications may want to put this into update of
                    // the game state
                    InputSystem.update();

                    // update game state, do not use interpolation parameter
                    update(-1.0f);

                    // render, do not use interpolation parameter
                    render(-1.0f);

                    // swap buffers
                    display.getRenderer().displayBackBuffer();

                    Thread.yield();
                }
            }
        } catch (Throwable t) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "start()", "Exception in game loop", t);
            if (throwableHandler != null) {
				throwableHandler.handle(t);
			}
        }

        cleanup();
        logger.info( "Application ending.");

        if (display != null)
            display.reset();
        quit();
    }

    /**
     * Closes the display
     * 
     * @see AbstractGame#quit()
     */
    protected void quit() {
        if (display != null)
            display.close();
    }

    /**
     * <code>assertDisplayCreated</code> determines if the display system
     * was successfully created before use.
     * @throws JmeException if the display system was not successfully created
     */
    protected void assertDisplayCreated() throws JmeException {
        if (display == null) {
            logger.severe( "Display system is null.");

            throw new JmeException("Window must be created during" + " initialization.");
        }
        if (!display.isCreated()) {
            logger.severe( "Display system not initialized.");

            throw new JmeException("Window must be created during" + " initialization.");
        }
    }


    
    /**
     * <code>finish</code> breaks out of the main game loop. It is preferable to
     * call <code>finish</code> instead of <code>quit</code>.
     */
    public void finish() {
      finished = true;
    }
    
	/**
	 *
	 * @return
	 */
	protected ThrowableHandler getThrowableHandler() {
		return throwableHandler;
	}

	/**
	 *
	 * @param throwableHandler
	 */
	protected void setThrowableHandler(ThrowableHandler throwableHandler) {
		this.throwableHandler = throwableHandler;
	}

    /**
     * @param interpolation
     *            unused in this implementation
     * @see AbstractGame#update(float interpolation)
     */
    protected abstract void update(float interpolation);

    /**
     * @param interpolation
     *            unused in this implementation
     * @see AbstractGame#render(float interpolation)
     */
    protected abstract void render(float interpolation);

    /**
     * @see AbstractGame#initSystem()
     */
    protected abstract void initSystem();

    /**
     * @see AbstractGame#initGame()
     */
    protected abstract void initGame();

    /**
     * @see AbstractGame#reinit()
     */
    protected abstract void reinit();

    /**
     * @see AbstractGame#cleanup()
     */
    protected abstract void cleanup();
}