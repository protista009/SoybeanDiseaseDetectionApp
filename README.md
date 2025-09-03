# 🌱 Soybean Disease Detection

An **Android app** that detects soybean leaf diseases using **deep learning (TensorFlow Lite)**.  
The app allows farmers and researchers to **capture or upload images** of soybean leaves, analyzes them with trained models, and provides **predictions with confidence scores**.  
It also supports **multiple languages** for better accessibility.  

---

## 🚀 Features

- 📷 **Image Input** – Capture or select leaf images  
- 🤖 **Three-stage ML pipeline**:  
  1. Leaf vs Not-Leaf detection  
  2. Healthy vs Unhealthy classification  
  3. Disease type classification (for unhealthy leaves)  
- 📊 **Prediction history** stored locally using Room Database  
- 🕒 **Timestamps** for all predictions  
- 🌐 **Multilingual support** – English, Hindi, Tamil, Telugu, Gujarati  
- 📱 **Modern UI** built with Material Design  

---

## 🛠️ Tech Stack

- **Android Studio (Java)**  
- **TensorFlow Lite** – ML inference (offline)  
- **Room Database** – Store prediction history  
- **Material Design Components** – UI/UX  

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the repo

```bash
git clone https://github.com/your-username/soybean-disease-detection.git
cd soybean-disease-detection
```
### 2️⃣ Open in Android Studio
- Launch Android Studio
- Click File > Open
- Select this project folder

### 3️⃣ Add ML models
- Place your .tflite models in the app/src/main/assets/ folder:
- leaf_notleaf.tflite
- health_classifier.tflite
- disease_classifier.tflite

### 4️⃣ Build & Run
- Connect a physical device or emulator
- Click Run ▶️ in Android Studio

 ---

 ## Contibution
 - Pooja Patel
 - Manaswi Mane
