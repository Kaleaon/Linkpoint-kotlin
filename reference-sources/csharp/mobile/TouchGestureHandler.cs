/**
 * @file TouchGestureHandler.cs
 * @brief Advanced touch gesture handling for mobile virtual world interaction
 * 
 * Mobile gesture system component
 * This is a reference implementation for translation to Kotlin gesture handling
 */

using System;
using System.Collections.Generic;
using System.Drawing;

namespace MobileViewer.Input
{
    public enum GestureType
    {
        None,
        Tap,
        DoubleTap,
        LongPress,
        Pan,
        Pinch,
        Rotate,
        Swipe,
        TwoFingerTap
    }

    public struct GestureEvent
    {
        public GestureType Type;
        public PointF Location;
        public PointF Delta;
        public float Scale;
        public float Rotation;
        public float Velocity;
        public TimeSpan Duration;
        public int FingerCount;
    }

    public class TouchGestureHandler
    {
        private List<TouchPoint> _activeTouches;
        private DateTime _gestureStartTime;
        private bool _gestureInProgress;
        private GestureType _currentGesture;
        private PointF _initialCenter;
        private float _initialDistance;
        private float _initialAngle;
        
        public event Action<GestureEvent> GestureDetected;
        
        public TouchGestureHandler()
        {
            _activeTouches = new List<TouchPoint>();
            _gestureInProgress = false;
            _currentGesture = GestureType.None;
        }
        
        /// <summary>
        /// Process touch input and detect gestures
        /// </summary>
        public void ProcessTouch(int pointerId, float x, float y, TouchPhase phase)
        {
            switch (phase)
            {
                case TouchPhase.Began:
                    HandleTouchBegan(pointerId, x, y);
                    break;
                case TouchPhase.Moved:
                    HandleTouchMoved(pointerId, x, y);
                    break;
                case TouchPhase.Ended:
                    HandleTouchEnded(pointerId, x, y);
                    break;
                case TouchPhase.Cancelled:
                    HandleTouchCancelled(pointerId);
                    break;
            }
        }
        
        private void HandleTouchBegan(int pointerId, float x, float y)
        {
            var touch = new TouchPoint
            {
                Id = pointerId,
                Position = new PointF(x, y),
                StartPosition = new PointF(x, y),
                StartTime = DateTime.Now,
                Phase = TouchPhase.Began
            };
            
            _activeTouches.Add(touch);
            
            if (_activeTouches.Count == 1)
            {
                // First touch - start gesture detection
                _gestureStartTime = DateTime.Now;
                _initialCenter = new PointF(x, y);
                StartGestureDetection();
            }
            else if (_activeTouches.Count == 2)
            {
                // Second touch - enable multi-touch gestures
                UpdateMultiTouchGeometry();
            }
            
            Console.WriteLine($"Touch began: ID={pointerId}, Position=({x}, {y}), Total touches={_activeTouches.Count}");
        }
        
        private void HandleTouchMoved(int pointerId, float x, float y)
        {
            var touch = _activeTouches.Find(t => t.Id == pointerId);
            if (touch == null) return;
            
            var previousPosition = touch.Position;
            touch.Position = new PointF(x, y);
            touch.Phase = TouchPhase.Moved;
            
            if (_activeTouches.Count == 1)
            {
                HandleSingleTouchMovement(touch, previousPosition);
            }
            else if (_activeTouches.Count == 2)
            {
                HandleMultiTouchMovement();
            }
        }
        
        private void HandleTouchEnded(int pointerId, float x, float y)
        {
            var touch = _activeTouches.Find(t => t.Id == pointerId);
            if (touch == null) return;
            
            touch.Position = new PointF(x, y);
            touch.Phase = TouchPhase.Ended;
            
            // Check for gesture completion
            CheckForGestureCompletion(touch);
            
            _activeTouches.RemoveAll(t => t.Id == pointerId);
            
            if (_activeTouches.Count == 0)
            {
                EndGestureDetection();
            }
            
            Console.WriteLine($"Touch ended: ID={pointerId}, Position=({x}, {y}), Remaining touches={_activeTouches.Count}");
        }
        
        private void HandleTouchCancelled(int pointerId)
        {
            _activeTouches.RemoveAll(t => t.Id == pointerId);
            
            if (_activeTouches.Count == 0)
            {
                EndGestureDetection();
            }
        }
        
        private void StartGestureDetection()
        {
            _gestureInProgress = true;
            _currentGesture = GestureType.None;
        }
        
        private void EndGestureDetection()
        {
            _gestureInProgress = false;
            _currentGesture = GestureType.None;
        }
        
        private void HandleSingleTouchMovement(TouchPoint touch, PointF previousPosition)
        {
            var delta = new PointF(
                touch.Position.X - previousPosition.X,
                touch.Position.Y - previousPosition.Y
            );
            
            var totalDelta = new PointF(
                touch.Position.X - touch.StartPosition.X,
                touch.Position.Y - touch.StartPosition.Y
            );
            
            var distance = Math.Sqrt(totalDelta.X * totalDelta.X + totalDelta.Y * totalDelta.Y);
            
            if (distance > 10) // Movement threshold
            {
                if (_currentGesture == GestureType.None)
                {
                    _currentGesture = GestureType.Pan;
                }
                
                if (_currentGesture == GestureType.Pan)
                {
                    EmitGestureEvent(new GestureEvent
                    {
                        Type = GestureType.Pan,
                        Location = touch.Position,
                        Delta = delta,
                        Duration = DateTime.Now - _gestureStartTime,
                        FingerCount = 1
                    });
                }
            }
        }
        
        private void HandleMultiTouchMovement()
        {
            if (_activeTouches.Count != 2) return;
            
            var touch1 = _activeTouches[0];
            var touch2 = _activeTouches[1];
            
            var center = new PointF(
                (touch1.Position.X + touch2.Position.X) / 2,
                (touch1.Position.Y + touch2.Position.Y) / 2
            );
            
            var distance = CalculateDistance(touch1.Position, touch2.Position);
            var angle = CalculateAngle(touch1.Position, touch2.Position);
            
            // Detect pinch gesture
            if (Math.Abs(distance - _initialDistance) > 20)
            {
                var scale = distance / _initialDistance;
                
                EmitGestureEvent(new GestureEvent
                {
                    Type = GestureType.Pinch,
                    Location = center,
                    Scale = scale,
                    Duration = DateTime.Now - _gestureStartTime,
                    FingerCount = 2
                });
            }
            
            // Detect rotation gesture
            var angleDelta = angle - _initialAngle;
            if (Math.Abs(angleDelta) > 5) // 5 degree threshold
            {
                EmitGestureEvent(new GestureEvent
                {
                    Type = GestureType.Rotate,
                    Location = center,
                    Rotation = angleDelta,
                    Duration = DateTime.Now - _gestureStartTime,
                    FingerCount = 2
                });
            }
        }
        
        private void CheckForGestureCompletion(TouchPoint touch)
        {
            var duration = DateTime.Now - touch.StartTime;
            var distance = CalculateDistance(touch.Position, touch.StartPosition);
            
            if (distance < 10) // Small movement threshold
            {
                if (duration.TotalMilliseconds < 200)
                {
                    // Quick tap
                    EmitGestureEvent(new GestureEvent
                    {
                        Type = GestureType.Tap,
                        Location = touch.Position,
                        Duration = duration,
                        FingerCount = _activeTouches.Count
                    });
                }
                else if (duration.TotalMilliseconds > 1000)
                {
                    // Long press
                    EmitGestureEvent(new GestureEvent
                    {
                        Type = GestureType.LongPress,
                        Location = touch.Position,
                        Duration = duration,
                        FingerCount = _activeTouches.Count
                    });
                }
            }
            else if (distance > 50) // Significant movement
            {
                // Swipe gesture
                var velocity = (float)(distance / duration.TotalMilliseconds);
                
                EmitGestureEvent(new GestureEvent
                {
                    Type = GestureType.Swipe,
                    Location = touch.Position,
                    Delta = new PointF(
                        touch.Position.X - touch.StartPosition.X,
                        touch.Position.Y - touch.StartPosition.Y
                    ),
                    Velocity = velocity,
                    Duration = duration,
                    FingerCount = 1
                });
            }
        }
        
        private void UpdateMultiTouchGeometry()
        {
            if (_activeTouches.Count != 2) return;
            
            var touch1 = _activeTouches[0];
            var touch2 = _activeTouches[1];
            
            _initialCenter = new PointF(
                (touch1.Position.X + touch2.Position.X) / 2,
                (touch1.Position.Y + touch2.Position.Y) / 2
            );
            
            _initialDistance = CalculateDistance(touch1.Position, touch2.Position);
            _initialAngle = CalculateAngle(touch1.Position, touch2.Position);
        }
        
        private float CalculateDistance(PointF p1, PointF p2)
        {
            var dx = p2.X - p1.X;
            var dy = p2.Y - p1.Y;
            return (float)Math.Sqrt(dx * dx + dy * dy);
        }
        
        private float CalculateAngle(PointF p1, PointF p2)
        {
            var dx = p2.X - p1.X;
            var dy = p2.Y - p1.Y;
            return (float)(Math.Atan2(dy, dx) * 180 / Math.PI);
        }
        
        private void EmitGestureEvent(GestureEvent gestureEvent)
        {
            Console.WriteLine($"Gesture detected: {gestureEvent.Type} at ({gestureEvent.Location.X}, {gestureEvent.Location.Y})");
            GestureDetected?.Invoke(gestureEvent);
        }
    }
    
    public class TouchPoint
    {
        public int Id { get; set; }
        public PointF Position { get; set; }
        public PointF StartPosition { get; set; }
        public DateTime StartTime { get; set; }
        public TouchPhase Phase { get; set; }
    }
    
    public enum TouchPhase
    {
        Began,
        Moved,
        Ended,
        Cancelled
    }
}