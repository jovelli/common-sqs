package com.taxis99.sqs

trait SqsConsumer[T] {
  def consumer(message: T)
}
