import numpy as np
from scipy.io.wavfile import write
import os

def generate_wooden_wheel():
    # Set parameters
    sample_rate = 44100  # Standard audio sample rate
    duration = 1.0       # Adjusted to 1.0 seconds to match movement step duration better
    frequency = 40.0     # Low frequency for a heavy rumble

    # Generate time axis
    t = np.linspace(0, duration, int(sample_rate * duration), False)

    # Generate a base low-frequency rumble (sine wave)
    rumble = np.sin(frequency * t * 2 * np.pi)

    # Add brown noise to simulate grinding friction
    noise = np.random.normal(0, 1, len(t))
    brown_noise = np.cumsum(noise)
    brown_noise = brown_noise / np.max(np.abs(brown_noise)) # Normalize

    # Combine rumble and noise to create a heavy grinding texture
    combined_signal = (rumble * 0.6) + (brown_noise * 0.4)

    # Apply a simulated slow rhythmic "thud" for the wheel rotation
    modulation = (np.sin(2 * t * 2 * np.pi) + 1) / 2
    final_signal = combined_signal * (0.5 + 0.5 * modulation)

    # Convert to 16-bit PCM format
    audio_data = np.int16(final_signal * 32767)

    # Export as .wav file to the proper directory
    filename = r"C:\Users\BASANTA\eclipse-workspace\TurnBasedGame\assets\bundled\audio\siege_move.wav"
    write(filename, sample_rate, audio_data)
    print(f"Saved as {filename}")

if __name__ == "__main__":
    generate_wooden_wheel()
