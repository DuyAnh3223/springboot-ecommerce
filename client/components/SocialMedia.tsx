import React from 'react'
import { FaYoutube, FaGithub, FaLinkedin, FaFacebook, FaSlack } from "react-icons/fa6";
import { Tooltip, TooltipProvider, TooltipTrigger, TooltipContent } from './ui/tooltip';
import { cn } from '@/lib/utils';
import Link from 'next/link';

interface Props {
    className?:string;
    iconClassName?: string;
    tooltipClassName?: string;
}

const socialLink = [
  {
    title: "Youtube",
    href: "https://www.youtube.com/",
    icon: <FaYoutube className="w-5 h-5" />,
  },
  {
    title: "Github",
    href: "https://www.youtube.com/",
    icon: <FaGithub className="w-5 h-5" />,
  },
  {
    title: "Linkedin",
    href: "https://www.youtube.com/",
    icon: <FaLinkedin className="w-5 h-5" />,
  },
  {
    title: "Facebook",
    href: "https://www.youtube.com/",
    icon: <FaFacebook className="w-5 h-5" />,
  },
  {
    title: "Slack",
    href: "https://www.youtube.com/",
    icon: <FaSlack className="w-5 h-5" />,
  },
];



const SocialMedia = ({className, iconClassName, tooltipClassName}:Props) => {


  return (
    <TooltipProvider>
        <div className={cn("flex items-center gap-3.5", className)}>
            {socialLink?.map((item)=>(
                <Tooltip key={item?.title}>
                    <TooltipTrigger
                      render={
                        <Link 
                          target="_blank"  
                          rel="noopener noreferrer"
                          href={item?.href}
                          className={cn("p-2 border rounded-full hover:border-shop_light_green hoverEffect", iconClassName)}
                        />
                      }
                    >
                      {item?.icon}
                    </TooltipTrigger>
                    <TooltipContent className={cn("bg-white text-darkColor font-semibold ",tooltipClassName)}>
                      {item?.title}
                    </TooltipContent>
                </Tooltip>
            ))}
        </div>
    </TooltipProvider>
  )
}

export default SocialMedia