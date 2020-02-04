import React, { Component } from 'react';

class Footer extends Component {
    render() {
        return (
            <footer role="banner">
      
                <section>
                    <div id="footer-v2" className="footer-v2">
                        <div className="footer">
                            <div className="container">
                                <div className="row">
                                    
                                    <div className="col-md-8 md-margin-bottom-40">
                                        <script src="http://cdn.lightwidget.com/widgets/lightwidget.js"></script>
                                        <iframe title="Instagram" src="//lightwidget.com/widgets/aa5ba80ed03c5bc187d1419e2a57bac8.html" 
                                          scrolling="no" allowtransparency="true" className="lightwidget-widget" 
                                          style = {{
                                              width: '100%',
                                              border: '0',
                                              overflow: 'hidden'
                                          }}>
                                        </iframe>
                                    </div>
                                    <div className="col-md-4 md-margin-bottom-40">
                                        <div className="headline"><h2 className="heading-sm">Contact Us</h2></div>
                                        <ul className="social-icons">
                                            <li className="VC_social_icon"><a href="https://www.instagram.com/peregin" data-original-title="Instagram" className="rounded-x social_instagram">Instagram</a><small>INSTAGRAM</small></li>
                                            <li className="VC_social_icon"><a href="https://github.com/peregin/velocorner.com" data-original-title="Github" className="rounded-x social_github">GitHub</a><small>GITHUB</small></li>
                                            <li className="VC_social_icon"><a href="https://www.strava.com/clubs/velocorner" data-original-title="Strava" className="rounded-x social_strava">Strava</a><small>STRAVA</small></li>
                                            <li className="VC_social_icon"><a href="https://www.youtube.com/channel/UCekBQAfLHviXTvB1kVjzBZQ" data-original-title="Youtube" className="rounded-x social_youtube">Youtube</a><small>YOUTUBE</small></li>
                                            <li className="VC_social_icon"><a href="https://twitter.com/velocorner_com" data-original-title="Twitter" className="rounded-x social_twitter">Twitter</a><small>TWITTER</small></li>
                                            <li className="VC_social_icon"><a href="mailto:velocorner.com@@gmail.com" data-original-title="Email" className="rounded-x social_email">velocorner.com@@gmail.com</a><small>EMAIL</small></li>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="copyright">
                            <div className="container">
                                <p className="text-center">2015 - {(new Date().getFullYear())} &copy; All Rights Reserved <a target="_blank" rel="noopener noreferrer" href="http://velocorner.com">Velocorner</a></p>
                            </div>
                        </div>

                    </div>
                </section>

            </footer>
        )
    }
}

export default Footer;