const app = document.getElementById('root');

const logo = document.createElement('img');
logo.src = 'logo.png';

const container = document.createElement('div');
container.setAttribute('class', 'container');

app.appendChild(logo);
app.appendChild(container);

var request = new XMLHttpRequest();

//request.open('GET', 'https://ghibliapi.herokuapp.com/films', true);

request.open('GET', 'http://ds0.example.com:8080/api/users?_queryFilter=true');
//request.send(null);
//request.setRequestHeader('X-OpenIDM-Username', 'user.9');
//request.setRequestHeader('X-OpenIDM-Password', 'password');
request.setRequestHeader("Authorization", "Basic " + btoa("user.9:password"));
request.withCredentials = true;

request.onload = function () {

  // Begin accessing JSON data here
  var data = JSON.parse("[" + this.response + "]");
  if (request.status >= 200 && request.status < 400) {
      console.log(data);
//      errorMessage.textContent = data;
//      app.appendChild(errorMessage);
    data.forEach(movie => {
      const card = document.createElement('div');
      card.setAttribute('class', 'card');

      const h1 = document.createElement('h1');
      h1.textContent = movie.userName;

      const p = document.createElement('p');
      movie.description = movie.description.substring(0, 300);
      p.textContent = `${movie.description}...`;

      container.appendChild(card);
      card.appendChild(h1);
      card.appendChild(p);
    });
  } else {
    const errorMessage = document.createElement('marquee');
    errorMessage.textContent = `Gah, it's not working!`;
    app.appendChild(errorMessage);
  }
}

request.send();