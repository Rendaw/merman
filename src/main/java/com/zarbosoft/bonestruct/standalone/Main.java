package com.zarbosoft.bonestruct.standalone;

import com.zarbosoft.bonestruct.editor.Editor;
import com.zarbosoft.bonestruct.editor.visual.IdleTask;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.PriorityQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
	private final IdleTask idleCompact = null;

	public static void main(final String[] args) {
		launch(args);
	}

	ScheduledThreadPoolExecutor worker = new ScheduledThreadPoolExecutor(1);
	boolean idlePending = false;
	ScheduledFuture<?> idleTimer = null;
	public PriorityQueue<IdleTask> idleQueue = new PriorityQueue<>();

	@Override
	public void start(final Stage primaryStage) {
		/*
		if (getParameters().getUnnamed().isEmpty())
			throw new IllegalArgumentException("Must specify a filename as first argument.");
			*/
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(final WindowEvent t) {
				worker.shutdown();
			}
		});
		//final Editor editor = new Editor(this::addIdle, getParameters().getUnnamed().get(0));
		final Editor editor = new Editor(this::addIdle, "todo");
		final Scene scene = new Scene(editor.getVisual(), 300, 275);
		scene.setOnKeyPressed(event -> {
			editor.handleKey(event);
		});
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void addIdle(final IdleTask task) {
		idleQueue.add(task);
		if (idleTimer == null) {
			try {
				idleTimer = worker.scheduleWithFixedDelay(() -> {
					//System.out.println("idle timer");
					if (idlePending)
						return;
					idlePending = true;
					Platform.runLater(() -> {
						//System.out.println(String.format("idle timer inner: %d", idleQueue.size()));
						// TODO measure pending event backlog, adjust batch size to accomodate
						// by proxy? time since last invocation?
						for (int i = 0; i < 1000; ++i) { // Batch
							final IdleTask top = idleQueue.poll();
							if (top == null) {
								idleTimer.cancel(false);
								idleTimer = null;
								System.out.format("Idle stopping at %d\n", i);
								break;
							} else
								top.run();
						}
						//System.out.format("Idle break at g i %d\n", GroupVisualNode.idleCount);
						idlePending = false;
					});
				}, 0, 50, TimeUnit.MILLISECONDS);
			} catch (final RejectedExecutionException e) {
				// Happens on unhover when window closes to shutdown
			}
		}
	}
}