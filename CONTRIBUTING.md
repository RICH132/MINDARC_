# Contributing to MindArc

Welcome to MindArc! We're excited that you're interested in contributing to our digital wellbeing project. This guide will help you get started, even if you're new to open source.

## 🌟 How to Contribute

There are many ways to contribute:
- **Reporting Bugs:** Found something that doesn't work? Let us know.
- **Suggesting Features:** Have a great idea for a new activity or UI improvement?
- **Improving Documentation:** Fix typos or make the guides clearer.
- **Writing Code:** Help us implement new features or fix existing bugs.

---

## 🛠️ Step-by-Step Guide

### 1. Find or Create an Issue
Before you start working, check the [Issues](https://github.com/your-username/MindArc/issues) tab on GitHub.
- If you find an issue you want to work on, leave a comment asking to be assigned.
- If you have a new idea or found a bug, click **"New Issue"** and describe it clearly.

### 2. Fork the Project
Click the **"Fork"** button at the top right of the GitHub page. This creates a copy of the project in your own GitHub account.

### 3. Clone Your Fork
Open your terminal (or Git Bash) and run:
```bash
git clone https://github.com/YOUR-USERNAME/MindArc.git
```
*(Replace `YOUR-USERNAME` with your actual GitHub username)*

### 4. Create a Branch
Never work directly on the `main` branch. Create a new "branch" for your specific task:
```bash
git checkout -b feature/your-feature-name
# OR for bugs
git checkout -b fix/bug-description
```
*Tip: Give it a short, descriptive name.*

### 5. Make Changes & Test
Open the project in Android Studio, make your changes, and ensure the app builds and runs correctly.

### 6. Commit Your Changes
Save your progress with a "commit message" that explains what you did:
```bash
git add .
git commit -m "feat: added a new timer to reading screen"
```

### 7. Push to GitHub
Upload your branch to your fork:
```bash
git push origin feature/your-feature-name
```

### 8. Create a Pull Request (PR)
1. Go to the original MindArc repository on GitHub.
2. You should see a yellow banner saying **"Compare & pull request"**. Click it!
3. Describe what you changed and reference the issue number (e.g., "Closes #12").
4. Submit the PR.

---

## 🚦 PR Review Process
Once you submit a PR:
- Maintainers will review your code.
- They might suggest some changes. Don't worry—this is a normal part of collaborating!
- Once everything looks good, your code will be merged into the main project.

## 📝 General Rules
- **Keep it clean:** Try to follow the existing coding style.
- **One PR per task:** Don't mix multiple unrelated changes in one PR.
- **Be respectful:** We are a community of learners and builders.

Thank you for making MindArc better! 🚀
