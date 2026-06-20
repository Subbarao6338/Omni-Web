(function() {
    const article = document.querySelector('article') || document.body;
    const title = document.title;
    const content = article.innerText;
    return JSON.stringify({ title: title, content: content });
})();
