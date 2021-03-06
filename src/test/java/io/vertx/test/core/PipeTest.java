/*
 * Copyright (c) 2011-2017 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.test.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.streams.Pipe;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

/**
 * 
 * @author arnaud le roy
 *
 */
public class PipeTest {

	@Test
	public void testPipeBasic() throws Exception {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		FakeWriteStream<MyClass> ws = new FakeWriteStream<>();
		ws.setWriteQueueMaxSize(1001);
		Pipe p = Pipe.create(rs).pipe(ws);

		for (int i = 0; i < 10; i++) { // Repeat a few times
			p.start();

			List<MyClass> inp = new ArrayList<>();
			for (int j = 0; j < 10; j++) {
				MyClass myClass = new MyClass();
				inp.add(myClass);
				rs.addData(myClass);
			}
			assertEquals(inp, ws.received);
			assertFalse(rs.paused);
			assertEquals(0, rs.pauseCount);
			assertEquals(0, rs.resumeCount);

			p.stop();
			ws.clearReceived();
			MyClass myClass = new MyClass();
			rs.addData(myClass);
			assertEquals(0, ws.received.size());
		}
	}

	@Test
	public void testPipeMultiple() throws Exception {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		FakeTransformStream<MyClass> ts = new FakeTransformStream<>();
		FakeWriteStream<MyClass> ws = new FakeWriteStream<>();
		ws.setWriteQueueMaxSize(1001);
		ts.setWriteQueueMaxSize(1001);
		Pipe p = Pipe.create(rs).pipe(ts).pipe(ws);

		for (int i = 0; i < 10; i++) { // Repeat a few times
			p.start();

			List<MyClass> inp = new ArrayList<>();
			for (int j = 0; j < 10; j++) {
				MyClass myClass = new MyClass();
				inp.add(myClass);
				rs.addData(myClass);
			}
			assertEquals(inp, ws.received);
			assertFalse(rs.paused);
			assertEquals(0, rs.pauseCount);
			assertEquals(0, rs.resumeCount);

			p.stop();
			ws.clearReceived();
			MyClass myClass = new MyClass();
			rs.addData(myClass);
			assertEquals(0, ws.received.size());
		}
	}

	
	@Test
	public void testPipePauseResume() throws Exception {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		FakeWriteStream<MyClass> ws = new FakeWriteStream<>();
		ws.setWriteQueueMaxSize(5);
		Pipe p = Pipe.create(rs).pipe(ws);
		p.start();

		for (int i = 0; i < 10; i++) {   // Repeat a few times
			List<MyClass> inp = new ArrayList<>();
			for (int j = 0; j < 4; j++) {
				MyClass myClass = new MyClass();
				inp.add(myClass);
				rs.addData(myClass);
				assertFalse(rs.paused);
				assertEquals(i, rs.pauseCount);
				assertEquals(i, rs.resumeCount);
			}
			MyClass myClass = new MyClass();
			inp.add(myClass);
			rs.addData(myClass);
			assertTrue(rs.paused);
			assertEquals(i + 1, rs.pauseCount);
			assertEquals(i, rs.resumeCount);

			assertEquals(inp, ws.received);
			ws.clearReceived();
			assertFalse(rs.paused);
			assertEquals(i + 1, rs.pauseCount);
			assertEquals(i + 1, rs.resumeCount);
		}
	}

	@Test(expected = NullPointerException.class)
	public void testPipeWriteStreamNull() {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		Pipe.create(rs).pipe(null);
	}

	@Test(expected = VertxException.class)
	public void testPipeEnded() {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		FakeWriteStream<MyClass> ws = new FakeWriteStream<>();
		Pipe p = Pipe.create(rs).pipe(ws).pipe(ws);
	}
	
	@Test
	public void testPipeEndWriteStream() {
		FakeReadStream<MyClass> rs = new FakeReadStream<>();
		FakeWriteStream<MyClass> ws = new FakeWriteStream<>();
		Pipe p = Pipe.create(rs).pipe(ws);
		p.start();
		rs.end();
		assertEquals(true, ws.ended);
	}
	
	
	private class FakeReadStream<T> implements ReadStream<T> {

		private Handler<T> dataHandler;
		private Handler<T> endHandler;
		private boolean paused;
		int pauseCount;
		int resumeCount;

		void addData(T data) {
			if (dataHandler != null) {
				dataHandler.handle(data);
			}
		}

		void end() {
			if (endHandler != null) {
				endHandler.handle(null);
			}
		}
		
		public FakeReadStream handler(Handler<T> handler) {
			this.dataHandler = handler;
			return this;
		}

		public FakeReadStream pause() {
			paused = true;
			pauseCount++;
			return this;
		}

		public FakeReadStream pause(Handler<Void> doneHandler) {
			pause();
			doneHandler.handle(null);
			return this;
		}

		public FakeReadStream resume() {
			paused = false;
			resumeCount++;
			return this;
		}

		public FakeReadStream resume(Handler<Void> doneHandler) {
			resume();
			doneHandler.handle(null);
			return this;
		}

		public FakeReadStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		public FakeReadStream endHandler(Handler endHandler) {
			this.endHandler = endHandler;
			return this;
		}
	}

	private class FakeWriteStream<T> implements WriteStream<T> {

		int maxSize;
		public Boolean ended = false;
		List<T> received = new ArrayList<>();
		Handler<Void> drainHandler;

		void clearReceived() {
			boolean callDrain = writeQueueFull();
			received = new ArrayList<>();
			if (callDrain && drainHandler != null) {
				drainHandler.handle(null);
			}
		}

		public FakeWriteStream setWriteQueueMaxSize(int maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		public boolean writeQueueFull() {
			return received.size() >= maxSize;
		}

		public FakeWriteStream drainHandler(Handler<Void> handler) {
			this.drainHandler = handler;
			return this;
		}

		public FakeWriteStream write(T data) {
			received.add(data);
			return this;
		}

		public FakeWriteStream exceptionHandler(Handler<Throwable> handler) {
			return this;
		}

		@Override
		public void end() {
			ended = true;
		}
	}

	private class FakeTransformStream<T> implements ReadStream<T>, WriteStream<T> {
		List<T> received = new ArrayList<>();
		private Handler<T> dataHandler;
		private Handler<Void> endHandler;
		private Handler<Throwable> exceptionHandler;
		private int maxSize;
		private Handler<Void> drainHandler;
		
		@Override
		public FakeTransformStream exceptionHandler(Handler<Throwable> handler) {
			this.exceptionHandler = handler;
			return this;
		}

		@Override
		public FakeTransformStream write(T data) {

			
			if (dataHandler != null) {
				dataHandler.handle(data);
			}
			return this;
		}

		@Override
		public void end() {
			if (endHandler != null) {
				endHandler.handle(null);
			}
		}

		@Override
		public FakeTransformStream setWriteQueueMaxSize(int maxSize) {
			this.maxSize = maxSize;
			return this;
		}

		@Override
		public boolean writeQueueFull() {
			return received.size() >= maxSize;
		}

		@Override
		public FakeTransformStream drainHandler(@Nullable Handler<Void> handler) {
			this.drainHandler = handler;
			return this;
		}

		@Override
		public FakeTransformStream handler(@Nullable Handler<T> handler) {
			this.dataHandler = handler;
			return this;
		}

		@Override
		public FakeTransformStream pause() {
			return this;
		}

		@Override
		public FakeTransformStream resume() {
			return this;
		}

		@Override
		public FakeTransformStream endHandler(@Nullable Handler endHandler) {
			this.endHandler = endHandler;
			return this;
		}
	}
	
	static class MyClass {

	}
}
