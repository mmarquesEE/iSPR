# Task: Optimize Camera Processing Pipeline for Memory and Speed

## Context

The current`CameraStreamManager` and `ProcessingFrameProcessor` are suffering from high memory churn
and CPU overhead. This is caused by frequent object allocation (boxing), list slicing, and
unnecessary array conversions during real-time image analysis (30 FPS).

## Objective

Refactor the camera processing logic to achieve "Zero-Allocation" in the hot path.

## Instructions

### 1. Refactor `ProcessingFrameProcessor.kt` (Logic Layer)

* **Primitive Migration:** Change all `liveTimeBuffer` collections (R, G, B, and Timestamps) from
  `MutableList<Int>` or `MutableList<Long>` to primitive `IntArray` and `LongArray`. Pre-allocate
  these with a fixed capacity based on `params.maxTimeBufferSize`.
* **Remove Slicing:** Eliminate all calls to `.slice(...).toIntArray()`. These are extremely
  expensive. Replace them with `System.arraycopy()` or `IntArray.copyOfRange()`.
* **Update Data Classes:** Modify `ChannelData` (and related results classes) to handle primitive
  `IntArray` references. If possible, pass a `size` parameter so the UI draws only the valid portion
  of a pre-allocated buffer.
* **Eliminate Boxing:** Ensure YUV-to-RGB calculations and timestamp recordings write directly into
  the primitive arrays by index to avoid `Integer` or `Long` object wrapping.

### 2. Refactor `CameraStreamManager.kt` (Camera Layer)

* **Buffer Reuse:** Ensure that the `ImageReader` listener is lean. Any intermediate arrays used to
  store plane data must be pre-allocated class properties, not local variables created inside
  `onImageAvailable`.
* **Direct Access:** Use `ByteBuffer.get(targetByteArray)` for bulk transfers from the camera planes
  to avoid manual iteration or `.array()` calls on DirectBuffers.
* **Safe Close:** Ensure `image.close()` is always called in a `finally` block or immediately after
  processing to prevent the `ImageReader` from stalling.

### 3. General Requirements

* **Code Quality:** Reformat the code using standard Kotlin style guides (fix indentation/spacing).
* **Documentation:** Add brief comments identifying the "Hot Path" to warn against future
  allocations.
* **Performance Goal:** The code must execute without triggering Garbage Collection (GC) pauses. No
  `new` objects or boxed `Lists` should be created within the `processImage()` execution loop.
