package com.libre.spider.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发控制服务 - 动态调整并发数量
 */
@Slf4j
@Service
public class ConcurrencyControlService {

	private volatile Semaphore semaphore;

	private final AtomicInteger currentPermits = new AtomicInteger(1);

	private final AtomicInteger successRequests = new AtomicInteger(0);

	private final AtomicInteger failedRequests = new AtomicInteger(0);

	private volatile long lastAdjustTime = System.currentTimeMillis();

	private static final int MIN_PERMITS = 1;

	private static final int MAX_PERMITS = 10;

	private static final long ADJUST_INTERVAL = 30000; // 30秒调整一次

	public ConcurrencyControlService() {
		this.semaphore = new Semaphore(currentPermits.get());
	}

	/**
	 * 获取许可证
	 */
	public boolean acquirePermit(long timeoutMs) {
		try {
			boolean acquired = semaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
			if (!acquired) {
				log.debug("获取并发许可证超时: {}ms", timeoutMs);
			}
			return acquired;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * 释放许可证
	 */
	public void releasePermit() {
		semaphore.release();
	}

	/**
	 * 记录成功请求
	 */
	public void recordSuccess() {
		successRequests.incrementAndGet();
		adjustConcurrencyIfNeeded();
	}

	/**
	 * 记录失败请求
	 */
	public void recordFailure() {
		failedRequests.incrementAndGet();
		adjustConcurrencyIfNeeded();
	}

	/**
	 * 根据成功率动态调整并发数
	 */
	private void adjustConcurrencyIfNeeded() {
		long now = System.currentTimeMillis();
		if (now - lastAdjustTime < ADJUST_INTERVAL) {
			return;
		}

		int success = successRequests.getAndSet(0);
		int failed = failedRequests.getAndSet(0);
		int total = success + failed;

		if (total < 10) { // 样本数太少，不调整
			return;
		}

		double successRate = (double) success / total;
		int current = currentPermits.get();

		log.debug("并发控制统计: 成功={}, 失败={}, 成功率={:.2f}, 当前并发数={}", success, failed, successRate, current);

		if (successRate > 0.9 && current < MAX_PERMITS) {
			// 成功率高，增加并发数
			increaseConcurrency();
		}
		else if (successRate < 0.7 && current > MIN_PERMITS) {
			// 成功率低，减少并发数
			decreaseConcurrency();
		}

		lastAdjustTime = now;
	}

	/**
	 * 增加并发数
	 */
	private void increaseConcurrency() {
		int newPermits = Math.min(currentPermits.incrementAndGet(), MAX_PERMITS);
		semaphore.release(); // 增加一个许可证
		log.info("增加并发数: {}", newPermits);
	}

	/**
	 * 减少并发数
	 */
	private void decreaseConcurrency() {
		int newPermits = Math.max(currentPermits.decrementAndGet(), MIN_PERMITS);
		try {
			semaphore.acquire(); // 减少一个许可证
			log.info("减少并发数: {}", newPermits);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			currentPermits.incrementAndGet(); // 回滚
		}
	}

	/**
	 * 获取当前并发数
	 */
	public int getCurrentConcurrency() {
		return currentPermits.get();
	}

	/**
	 * 手动设置并发数
	 */
	public void setConcurrency(int permits) {
		if (permits < MIN_PERMITS || permits > MAX_PERMITS) {
			throw new IllegalArgumentException("并发数必须在 " + MIN_PERMITS + " 到 " + MAX_PERMITS + " 之间");
		}

		int current = currentPermits.get();
		int diff = permits - current;

		if (diff > 0) {
			// 增加许可证
			semaphore.release(diff);
		}
		else if (diff < 0) {
			// 减少许可证
			try {
				semaphore.acquire(-diff);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}

		currentPermits.set(permits);
		log.info("手动设置并发数: {}", permits);
	}

}